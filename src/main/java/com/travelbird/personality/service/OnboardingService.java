package com.travelbird.personality.service;

import com.travelbird.personality.dto.response.PartnerBirdResponse;
import com.travelbird.personality.dto.request.PersonalityAnswerRequest;
import com.travelbird.personality.dto.response.PersonalityOptionResponse;
import com.travelbird.personality.dto.response.PersonalityQuestionResponse;
import com.travelbird.personality.dto.response.PersonalityTestCompletedResponse;
import com.travelbird.personality.dto.response.PersonalityTestResponse;
import com.travelbird.personality.dto.request.PersonalityTestSubmissionRequest;
import com.travelbird.personality.dto.request.PersonalityTieBreakerRequest;
import com.travelbird.personality.dto.response.PersonalityTieBreakerRequiredResponse;
import com.travelbird.personality.domain.PersonalityAnswer;
import com.travelbird.personality.domain.PersonalityOption;
import com.travelbird.personality.domain.PersonalityQuestion;
import com.travelbird.personality.domain.PersonalitySubmission;
import com.travelbird.personality.domain.PersonalityTest;
import com.travelbird.user.domain.User;
import com.travelbird.common.enums.BirdType;
import com.travelbird.personality.domain.PersonalityResultStatus;
import com.travelbird.personality.domain.PersonalitySubmissionStatus;
import com.travelbird.common.enums.PersonalityTrait;
import com.travelbird.user.domain.UserStatus;
import com.travelbird.global.error.ApiException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.personality.repository.PersonalityAnswerRepository;
import com.travelbird.personality.repository.PersonalityOptionRepository;
import com.travelbird.personality.repository.PersonalityQuestionRepository;
import com.travelbird.personality.repository.PersonalitySubmissionRepository;
import com.travelbird.personality.repository.PersonalityTestRepository;
import com.travelbird.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OnboardingService {

    private record TraitProfile(BirdType birdType, String birdName, String description) {
    }

    private static final Map<PersonalityTrait, TraitProfile> TRAIT_PROFILES = Map.of(
            PersonalityTrait.REST, new TraitProfile(BirdType.OMOKNUNI, "오목눈이", "힐링/휴양"),
            PersonalityTrait.ACTIVITY, new TraitProfile(BirdType.MULCHONGSAE, "물총새", "액티비티/모험"),
            PersonalityTrait.CULTURE, new TraitProfile(BirdType.HOBANSAE, "호반새", "문화/예술"),
            PersonalityTrait.GOURMET, new TraitProfile(BirdType.DDAKSAE, "딱새", "미식/맛집"),
            PersonalityTrait.PHOTO, new TraitProfile(BirdType.DONGBAKSAE, "동박새", "감성/기록")
    );

    private static final Map<BirdType, PersonalityTrait> TRAIT_BY_BIRD_TYPE = TRAIT_PROFILES.entrySet().stream()
            .collect(Collectors.toMap(e -> e.getValue().birdType(), Map.Entry::getKey));

    private final UserRepository userRepository;
    private final PersonalityTestRepository personalityTestRepository;
    private final PersonalityQuestionRepository personalityQuestionRepository;
    private final PersonalityOptionRepository personalityOptionRepository;
    private final PersonalitySubmissionRepository personalitySubmissionRepository;
    private final PersonalityAnswerRepository personalityAnswerRepository;

    public OnboardingService(
            UserRepository userRepository,
            PersonalityTestRepository personalityTestRepository,
            PersonalityQuestionRepository personalityQuestionRepository,
            PersonalityOptionRepository personalityOptionRepository,
            PersonalitySubmissionRepository personalitySubmissionRepository,
            PersonalityAnswerRepository personalityAnswerRepository
    ) {
        this.userRepository = userRepository;
        this.personalityTestRepository = personalityTestRepository;
        this.personalityQuestionRepository = personalityQuestionRepository;
        this.personalityOptionRepository = personalityOptionRepository;
        this.personalitySubmissionRepository = personalitySubmissionRepository;
        this.personalityAnswerRepository = personalityAnswerRepository;
    }

    public PersonalityTestResponse getPersonalityTest(Long userId) {
        getActiveUser(userId);

        PersonalityTest activeTest = personalityTestRepository.findByActiveTrue()
                .orElseThrow(() -> new ApiException(ErrorCode.PERSONALITY_TEST_UNAVAILABLE));

        List<PersonalityQuestion> questions = personalityQuestionRepository
                .findByPersonalityTest_TestVersionOrderByQuestionOrderAsc(activeTest.getTestVersion());
        List<Long> questionIds = questions.stream().map(PersonalityQuestion::getQuestionId).toList();
        Map<Long, List<PersonalityOption>> optionsByQuestion = personalityOptionRepository
                .findByQuestion_QuestionIdIn(questionIds).stream()
                .collect(Collectors.groupingBy(option -> option.getQuestion().getQuestionId()));

        List<PersonalityQuestionResponse> questionResponses = new ArrayList<>();
        int questionOrder = 1;
        for (PersonalityQuestion question : questions) {
            List<PersonalityOption> shuffledOptions = new ArrayList<>(
                    optionsByQuestion.getOrDefault(question.getQuestionId(), List.of()));
            Collections.shuffle(shuffledOptions);

            List<PersonalityOptionResponse> optionResponses = new ArrayList<>();
            int optionOrder = 1;
            for (PersonalityOption option : shuffledOptions) {
                optionResponses.add(new PersonalityOptionResponse(option.getOptionId(), option.getText(), optionOrder++));
            }
            questionResponses.add(new PersonalityQuestionResponse(
                    question.getQuestionId(), question.getText(), questionOrder++, optionResponses));
        }

        return new PersonalityTestResponse(activeTest.getTestVersion(), questionResponses);
    }

    public PersonalityTestSubmissionResult submitPersonalityTest(Long userId, PersonalityTestSubmissionRequest request) {
        User user = getActiveUser(userId);

        List<PersonalityAnswerRequest> answers = request == null ? null : request.answers();
        if (answers == null || answers.size() != 8) {
            throw new ApiException(ErrorCode.INCOMPLETE_PERSONALITY_TEST);
        }
        Set<Long> questionIdsInRequest = answers.stream()
                .map(PersonalityAnswerRequest::questionId)
                .collect(Collectors.toSet());
        if (questionIdsInRequest.size() != 8) {
            throw new ApiException(ErrorCode.DUPLICATED_PERSONALITY_ANSWER);
        }

        PersonalityTest activeTest = personalityTestRepository.findByActiveTrue().orElse(null);
        if (activeTest == null || !activeTest.getTestVersion().equals(request.testVersion())) {
            throw new ApiException(ErrorCode.PERSONALITY_TEST_VERSION_MISMATCH);
        }

        List<PersonalityQuestion> questions = personalityQuestionRepository
                .findByPersonalityTest_TestVersionOrderByQuestionOrderAsc(activeTest.getTestVersion());
        Map<Long, PersonalityQuestion> questionById = questions.stream()
                .collect(Collectors.toMap(PersonalityQuestion::getQuestionId, q -> q));
        if (!questionById.keySet().equals(questionIdsInRequest)) {
            throw new ApiException(ErrorCode.INVALID_PERSONALITY_OPTION);
        }

        Map<Long, PersonalityOption> optionById = personalityOptionRepository
                .findByQuestion_QuestionIdIn(new ArrayList<>(questionById.keySet())).stream()
                .collect(Collectors.toMap(PersonalityOption::getOptionId, o -> o));
        for (PersonalityAnswerRequest answer : answers) {
            PersonalityOption option = optionById.get(answer.optionId());
            if (option == null || !option.getQuestion().getQuestionId().equals(answer.questionId())) {
                throw new ApiException(ErrorCode.INVALID_PERSONALITY_OPTION);
            }
        }

        Optional<PersonalitySubmission> existing = personalitySubmissionRepository.findByUser_UserId(userId);
        if (existing.isPresent() && existing.get().getStatus() == PersonalitySubmissionStatus.COMPLETED) {
            throw new ApiException(ErrorCode.PERSONALITY_TEST_ALREADY_COMPLETED);
        }

        PersonalitySubmission submission;
        if (existing.isPresent()) {
            submission = existing.get();
            personalityAnswerRepository.deleteAllBySubmissionId(submission.getSubmissionId());
        } else {
            submission = personalitySubmissionRepository.save(PersonalitySubmission.create(user, activeTest));
        }

        Map<PersonalityTrait, Integer> scores = new EnumMap<>(PersonalityTrait.class);
        for (PersonalityAnswerRequest answer : answers) {
            PersonalityOption option = optionById.get(answer.optionId());
            scores.merge(option.getTrait(), 1, Integer::sum);
            personalityAnswerRepository.save(
                    PersonalityAnswer.create(submission, questionById.get(answer.questionId()), answer.optionId()));
        }

        int maxScore = Collections.max(scores.values());
        List<PersonalityTrait> topTraits = scores.entrySet().stream()
                .filter(e -> e.getValue() == maxScore)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();

        if (topTraits.size() == 1) {
            PersonalityTrait winner = topTraits.get(0);
            TraitProfile profile = TRAIT_PROFILES.get(winner);
            submission.recordSingleWinner(scores, winner, profile.birdType());
            user.assignBirdType(profile.birdType());
            return new PersonalityTestSubmissionResult.Completed(new PersonalityTestCompletedResponse(
                    submission.getSubmissionId(), PersonalityResultStatus.COMPLETED, true,
                    profile.birdType(), profile.birdName(), winner, profile.description()));
        }

        submission.recordTie(scores, topTraits);
        return new PersonalityTestSubmissionResult.TieBreakerRequired(new PersonalityTieBreakerRequiredResponse(
                submission.getSubmissionId(), PersonalityResultStatus.TIE_BREAKER_REQUIRED, false, null, topTraits));
    }

    public PersonalityTestCompletedResponse submitTieBreaker(Long userId, PersonalityTieBreakerRequest request) {
        User user = getActiveUser(userId);

        if (request == null || request.submissionId() == null) {
            throw new ApiException(ErrorCode.PERSONALITY_SUBMISSION_ACCESS_DENIED);
        }
        PersonalitySubmission submission = personalitySubmissionRepository.findById(request.submissionId())
                .orElseThrow(() -> new ApiException(ErrorCode.PERSONALITY_SUBMISSION_ACCESS_DENIED));
        if (!submission.getUser().getUserId().equals(userId)) {
            throw new ApiException(ErrorCode.PERSONALITY_SUBMISSION_ACCESS_DENIED);
        }

        if (submission.getStatus() == PersonalitySubmissionStatus.COMPLETED) {
            if (submission.getTiedTraits() == null) {
                throw new ApiException(ErrorCode.TIE_BREAKER_NOT_REQUIRED);
            }
            throw new ApiException(ErrorCode.PERSONALITY_TEST_ALREADY_COMPLETED);
        }
        if (submission.getTiedTraits() == null || !submission.getTiedTraits().contains(request.selectedTrait())) {
            throw new ApiException(ErrorCode.INVALID_TIE_BREAKER_SELECTION);
        }

        TraitProfile profile = TRAIT_PROFILES.get(request.selectedTrait());
        submission.resolveTieBreaker(request.selectedTrait(), profile.birdType());
        user.assignBirdType(profile.birdType());

        return new PersonalityTestCompletedResponse(
                submission.getSubmissionId(), PersonalityResultStatus.COMPLETED, true,
                profile.birdType(), profile.birdName(), request.selectedTrait(), profile.description());
    }

    public PartnerBirdResponse getPartnerBird(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(ErrorCode.USER_NOT_ACTIVE);
        }

        if (user.getBirdType() == null) {
            return new PartnerBirdResponse(user.isOnboardingCompleted(), null, null, null, null);
        }
        PersonalityTrait trait = TRAIT_BY_BIRD_TYPE.get(user.getBirdType());
        TraitProfile profile = TRAIT_PROFILES.get(trait);
        return new PartnerBirdResponse(true, user.getBirdType(), profile.birdName(), trait, profile.description());
    }

    private User getActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_ACTIVE));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(ErrorCode.USER_NOT_ACTIVE);
        }
        return user;
    }
}
