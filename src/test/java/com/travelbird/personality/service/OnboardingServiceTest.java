package com.travelbird.personality.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.travelbird.personality.dto.request.PersonalityAnswerRequest;
import com.travelbird.personality.dto.request.PersonalityTestSubmissionRequest;
import com.travelbird.personality.dto.request.PersonalityTieBreakerRequest;
import com.travelbird.personality.domain.PersonalityOption;
import com.travelbird.personality.domain.PersonalityQuestion;
import com.travelbird.personality.domain.PersonalitySubmission;
import com.travelbird.personality.domain.PersonalityTest;
import com.travelbird.user.domain.User;
import com.travelbird.common.enums.BirdType;
import com.travelbird.personality.domain.PersonalitySubmissionStatus;
import com.travelbird.common.enums.PersonalityTrait;
import com.travelbird.user.domain.UserStatus;
import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.personality.repository.PersonalityAnswerRepository;
import com.travelbird.personality.repository.PersonalityOptionRepository;
import com.travelbird.personality.repository.PersonalityQuestionRepository;
import com.travelbird.personality.repository.PersonalitySubmissionRepository;
import com.travelbird.personality.repository.PersonalityTestRepository;
import com.travelbird.user.repository.UserRepository;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PersonalityTestRepository personalityTestRepository;
    @Mock
    private PersonalityQuestionRepository personalityQuestionRepository;
    @Mock
    private PersonalityOptionRepository personalityOptionRepository;
    @Mock
    private PersonalitySubmissionRepository personalitySubmissionRepository;
    @Mock
    private PersonalityAnswerRepository personalityAnswerRepository;

    private OnboardingService onboardingService;
    private User activeUser;
    private PersonalityTest activeTest;

    @BeforeEach
    void setUp() {
        onboardingService = new OnboardingService(
                userRepository, personalityTestRepository, personalityQuestionRepository,
                personalityOptionRepository, personalitySubmissionRepository, personalityAnswerRepository);

        activeUser = User.createFromKakao("user@kakao.com");
        ReflectionTestUtils.setField(activeUser, "userId", 1L);
        ReflectionTestUtils.setField(activeUser, "status", UserStatus.ACTIVE);
        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

        activeTest = newInstance(PersonalityTest.class);
        ReflectionTestUtils.setField(activeTest, "testVersion", "v1");
        ReflectionTestUtils.setField(activeTest, "active", true);
        lenient().when(personalityTestRepository.findByActiveTrue()).thenReturn(Optional.of(activeTest));

        lenient().when(personalitySubmissionRepository.save(any(PersonalitySubmission.class)))
                .thenAnswer(invocation -> {
                    PersonalitySubmission submission = invocation.getArgument(0);
                    if (submission.getSubmissionId() == null) {
                        ReflectionTestUtils.setField(submission, "submissionId", 100L);
                    }
                    return submission;
                });
    }

    @Test
    void getPersonalityTest_noActiveTest_throwsUnavailable() {
        when(personalityTestRepository.findByActiveTrue()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> onboardingService.getPersonalityTest(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PERSONALITY_TEST_UNAVAILABLE);
    }

    @Test
    void submitPersonalityTest_wrongAnswerCount_throwsIncomplete() {
        var request = new PersonalityTestSubmissionRequest("v1", List.of(new PersonalityAnswerRequest(1L, 101L)));

        assertThatThrownBy(() -> onboardingService.submitPersonalityTest(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INCOMPLETE_PERSONALITY_TEST);
    }

    @Test
    void submitPersonalityTest_duplicateQuestion_throwsDuplicated() {
        List<PersonalityAnswerRequest> answers = new ArrayList<>();
        answers.add(new PersonalityAnswerRequest(1L, 101L));
        for (long q = 1; q <= 7; q++) {
            answers.add(new PersonalityAnswerRequest(q, 100L + q));
        }
        var request = new PersonalityTestSubmissionRequest("v1", answers);

        assertThatThrownBy(() -> onboardingService.submitPersonalityTest(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATED_PERSONALITY_ANSWER);
    }

    @Test
    void submitPersonalityTest_versionMismatch_throwsVersionMismatch() {
        var request = new PersonalityTestSubmissionRequest("stale-version", optionAnswers());

        assertThatThrownBy(() -> onboardingService.submitPersonalityTest(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PERSONALITY_TEST_VERSION_MISMATCH);
    }

    @Test
    void submitPersonalityTest_invalidOption_throwsInvalidOption() {
        stubQuestionsAndOptions(PersonalityTrait.REST, PersonalityTrait.REST, PersonalityTrait.REST,
                PersonalityTrait.REST, PersonalityTrait.ACTIVITY, PersonalityTrait.ACTIVITY,
                PersonalityTrait.ACTIVITY, PersonalityTrait.ACTIVITY);

        List<PersonalityAnswerRequest> answers = new ArrayList<>();
        for (long q = 1; q <= 8; q++) {
            answers.add(new PersonalityAnswerRequest(q, 9999L));
        }
        var request = new PersonalityTestSubmissionRequest("v1", answers);

        assertThatThrownBy(() -> onboardingService.submitPersonalityTest(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PERSONALITY_OPTION);
    }

    @Test
    void submitPersonalityTest_singleWinner_completesAndAssignsBirdType() {
        stubQuestionsAndOptions(PersonalityTrait.REST, PersonalityTrait.REST, PersonalityTrait.REST,
                PersonalityTrait.ACTIVITY, PersonalityTrait.ACTIVITY, PersonalityTrait.CULTURE,
                PersonalityTrait.GOURMET, PersonalityTrait.PHOTO);
        when(personalitySubmissionRepository.findByUser_UserId(1L)).thenReturn(Optional.empty());
        var request = new PersonalityTestSubmissionRequest("v1", optionAnswers());

        PersonalityTestSubmissionResult result = onboardingService.submitPersonalityTest(1L, request);

        assertThat(result).isInstanceOf(PersonalityTestSubmissionResult.Completed.class);
        var completed = ((PersonalityTestSubmissionResult.Completed) result).response();
        assertThat(completed.trait()).isEqualTo(PersonalityTrait.REST);
        assertThat(completed.birdType()).isEqualTo(BirdType.OMOKNUNI);
        assertThat(completed.onboardingCompleted()).isTrue();
        assertThat(activeUser.getBirdType()).isEqualTo(BirdType.OMOKNUNI);
        assertThat(activeUser.isOnboardingCompleted()).isTrue();
    }

    @Test
    void submitPersonalityTest_tie_returnsTieBreakerRequired() {
        stubQuestionsAndOptions(PersonalityTrait.REST, PersonalityTrait.REST, PersonalityTrait.REST, PersonalityTrait.REST,
                PersonalityTrait.ACTIVITY, PersonalityTrait.ACTIVITY, PersonalityTrait.ACTIVITY, PersonalityTrait.ACTIVITY);
        when(personalitySubmissionRepository.findByUser_UserId(1L)).thenReturn(Optional.empty());
        var request = new PersonalityTestSubmissionRequest("v1", optionAnswers());

        PersonalityTestSubmissionResult result = onboardingService.submitPersonalityTest(1L, request);

        assertThat(result).isInstanceOf(PersonalityTestSubmissionResult.TieBreakerRequired.class);
        var tieBreaker = ((PersonalityTestSubmissionResult.TieBreakerRequired) result).response();
        assertThat(tieBreaker.tiedTraits()).containsExactlyInAnyOrder(PersonalityTrait.REST, PersonalityTrait.ACTIVITY);
        assertThat(tieBreaker.birdType()).isNull();
        assertThat(tieBreaker.onboardingCompleted()).isFalse();
        assertThat(activeUser.getBirdType()).isNull();
    }

    @Test
    void submitPersonalityTest_alreadyCompleted_throwsAlreadyCompleted() {
        stubQuestionsAndOptions(PersonalityTrait.REST, PersonalityTrait.REST, PersonalityTrait.REST,
                PersonalityTrait.ACTIVITY, PersonalityTrait.ACTIVITY, PersonalityTrait.CULTURE,
                PersonalityTrait.GOURMET, PersonalityTrait.PHOTO);
        PersonalitySubmission completedSubmission = PersonalitySubmission.create(activeUser, activeTest);
        ReflectionTestUtils.setField(completedSubmission, "submissionId", 55L);
        ReflectionTestUtils.setField(completedSubmission, "status", PersonalitySubmissionStatus.COMPLETED);
        when(personalitySubmissionRepository.findByUser_UserId(1L)).thenReturn(Optional.of(completedSubmission));
        var request = new PersonalityTestSubmissionRequest("v1", optionAnswers());

        assertThatThrownBy(() -> onboardingService.submitPersonalityTest(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PERSONALITY_TEST_ALREADY_COMPLETED);
    }

    @Test
    void submitPersonalityTest_concurrentDuplicateInsert_throwsAlreadyCompleted() {
        stubQuestionsAndOptions(PersonalityTrait.REST, PersonalityTrait.REST, PersonalityTrait.REST,
                PersonalityTrait.ACTIVITY, PersonalityTrait.ACTIVITY, PersonalityTrait.CULTURE,
                PersonalityTrait.GOURMET, PersonalityTrait.PHOTO);
        when(personalitySubmissionRepository.findByUser_UserId(1L)).thenReturn(Optional.empty());
        when(personalitySubmissionRepository.save(any(PersonalitySubmission.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("uk_personality_submission_user"));
        var request = new PersonalityTestSubmissionRequest("v1", optionAnswers());

        assertThatThrownBy(() -> onboardingService.submitPersonalityTest(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PERSONALITY_TEST_ALREADY_COMPLETED);
    }

    @Test
    void submitTieBreaker_notOwner_throwsAccessDenied() {
        User otherUser = User.createFromKakao(null);
        ReflectionTestUtils.setField(otherUser, "userId", 2L);
        PersonalitySubmission submission = PersonalitySubmission.create(otherUser, activeTest);
        ReflectionTestUtils.setField(submission, "submissionId", 55L);
        ReflectionTestUtils.setField(submission, "status", PersonalitySubmissionStatus.PENDING_TIE_BREAKER);
        ReflectionTestUtils.setField(submission, "tiedTraits", List.of(PersonalityTrait.REST, PersonalityTrait.ACTIVITY));
        when(personalitySubmissionRepository.findById(55L)).thenReturn(Optional.of(submission));

        var request = new PersonalityTieBreakerRequest(55L, PersonalityTrait.REST);

        assertThatThrownBy(() -> onboardingService.submitTieBreaker(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PERSONALITY_SUBMISSION_ACCESS_DENIED);
    }

    @Test
    void submitTieBreaker_invalidSelection_throwsInvalidSelection() {
        PersonalitySubmission submission = PersonalitySubmission.create(activeUser, activeTest);
        ReflectionTestUtils.setField(submission, "submissionId", 55L);
        ReflectionTestUtils.setField(submission, "status", PersonalitySubmissionStatus.PENDING_TIE_BREAKER);
        ReflectionTestUtils.setField(submission, "tiedTraits", List.of(PersonalityTrait.REST, PersonalityTrait.ACTIVITY));
        when(personalitySubmissionRepository.findById(55L)).thenReturn(Optional.of(submission));

        var request = new PersonalityTieBreakerRequest(55L, PersonalityTrait.CULTURE);

        assertThatThrownBy(() -> onboardingService.submitTieBreaker(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_TIE_BREAKER_SELECTION);
    }

    @Test
    void submitTieBreaker_validSelection_completesAndAssignsBirdType() {
        PersonalitySubmission submission = PersonalitySubmission.create(activeUser, activeTest);
        ReflectionTestUtils.setField(submission, "submissionId", 55L);
        ReflectionTestUtils.setField(submission, "status", PersonalitySubmissionStatus.PENDING_TIE_BREAKER);
        ReflectionTestUtils.setField(submission, "tiedTraits", List.of(PersonalityTrait.REST, PersonalityTrait.ACTIVITY));
        when(personalitySubmissionRepository.findById(55L)).thenReturn(Optional.of(submission));

        var request = new PersonalityTieBreakerRequest(55L, PersonalityTrait.ACTIVITY);

        var response = onboardingService.submitTieBreaker(1L, request);

        assertThat(response.trait()).isEqualTo(PersonalityTrait.ACTIVITY);
        assertThat(response.birdType()).isEqualTo(BirdType.MULCHONGSAE);
        assertThat(response.onboardingCompleted()).isTrue();
        assertThat(submission.getStatus()).isEqualTo(PersonalitySubmissionStatus.COMPLETED);
        assertThat(activeUser.getBirdType()).isEqualTo(BirdType.MULCHONGSAE);
    }

    @Test
    void submitTieBreaker_alreadyResolvedViaTie_throwsAlreadyCompleted() {
        PersonalitySubmission submission = PersonalitySubmission.create(activeUser, activeTest);
        ReflectionTestUtils.setField(submission, "submissionId", 55L);
        ReflectionTestUtils.setField(submission, "status", PersonalitySubmissionStatus.COMPLETED);
        ReflectionTestUtils.setField(submission, "tiedTraits", List.of(PersonalityTrait.REST, PersonalityTrait.ACTIVITY));
        when(personalitySubmissionRepository.findById(55L)).thenReturn(Optional.of(submission));

        var request = new PersonalityTieBreakerRequest(55L, PersonalityTrait.ACTIVITY);

        assertThatThrownBy(() -> onboardingService.submitTieBreaker(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PERSONALITY_TEST_ALREADY_COMPLETED);
    }

    @Test
    void submitTieBreaker_notRequiredForSingleWinnerSubmission_throwsNotRequired() {
        PersonalitySubmission submission = PersonalitySubmission.create(activeUser, activeTest);
        ReflectionTestUtils.setField(submission, "submissionId", 55L);
        ReflectionTestUtils.setField(submission, "status", PersonalitySubmissionStatus.COMPLETED);
        ReflectionTestUtils.setField(submission, "tiedTraits", null);
        when(personalitySubmissionRepository.findById(55L)).thenReturn(Optional.of(submission));

        var request = new PersonalityTieBreakerRequest(55L, PersonalityTrait.REST);

        assertThatThrownBy(() -> onboardingService.submitTieBreaker(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TIE_BREAKER_NOT_REQUIRED);
    }

    @Test
    void getPartnerBird_beforeOnboarding_returnsNullFields() {
        var response = onboardingService.getPartnerBird(1L);

        assertThat(response.onboardingCompleted()).isFalse();
        assertThat(response.birdType()).isNull();
        assertThat(response.trait()).isNull();
    }

    @Test
    void getPartnerBird_afterOnboarding_returnsProfile() {
        activeUser.assignBirdType(BirdType.HOBANSAE);

        var response = onboardingService.getPartnerBird(1L);

        assertThat(response.onboardingCompleted()).isTrue();
        assertThat(response.birdType()).isEqualTo(BirdType.HOBANSAE);
        assertThat(response.trait()).isEqualTo(PersonalityTrait.CULTURE);
        assertThat(response.birdName()).isEqualTo("호반새");
    }

    @Test
    void getPartnerBird_userNotFound_throwsUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> onboardingService.getPartnerBird(99L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void getPartnerBird_suspendedUser_throwsUserNotActive() {
        ReflectionTestUtils.setField(activeUser, "status", UserStatus.SUSPENDED);

        assertThatThrownBy(() -> onboardingService.getPartnerBird(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_ACTIVE);
    }

    private void stubQuestionsAndOptions(PersonalityTrait... traitsByQuestion) {
        List<PersonalityQuestion> questions = new ArrayList<>();
        List<PersonalityOption> options = new ArrayList<>();
        for (int i = 0; i < traitsByQuestion.length; i++) {
            long questionId = i + 1L;
            PersonalityQuestion question = newInstance(PersonalityQuestion.class);
            ReflectionTestUtils.setField(question, "questionId", questionId);
            ReflectionTestUtils.setField(question, "questionOrder", i + 1);
            ReflectionTestUtils.setField(question, "text", "Q" + questionId);
            questions.add(question);

            PersonalityOption option = newInstance(PersonalityOption.class);
            ReflectionTestUtils.setField(option, "optionId", 100L + questionId);
            ReflectionTestUtils.setField(option, "question", question);
            ReflectionTestUtils.setField(option, "text", "option-" + questionId);
            ReflectionTestUtils.setField(option, "trait", traitsByQuestion[i]);
            options.add(option);
        }
        when(personalityQuestionRepository.findByPersonalityTest_TestVersionOrderByQuestionOrderAsc("v1"))
                .thenReturn(questions);
        when(personalityOptionRepository.findByQuestion_QuestionIdIn(any())).thenReturn(options);
    }

    private List<PersonalityAnswerRequest> optionAnswers() {
        List<PersonalityAnswerRequest> answers = new ArrayList<>();
        for (long q = 1; q <= 8; q++) {
            answers.add(new PersonalityAnswerRequest(q, 100L + q));
        }
        return answers;
    }

    @SuppressWarnings("unchecked")
    private static <T> T newInstance(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
