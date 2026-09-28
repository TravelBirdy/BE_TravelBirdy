package com.travelbird.post.service;

import com.travelbird.file.api.FileLinkService;
import com.travelbird.post.controller.dto.CreatePostRequest;
import com.travelbird.post.domain.PostVisibility;
import com.travelbird.post.repository.PostHashtagRepository;
import com.travelbird.post.repository.PostImageRepository;
import com.travelbird.post.repository.PostPlaceRepository;
import com.travelbird.post.repository.PostRepository;
import com.travelbird.trip.api.TripPostReader;
import com.travelbird.user.api.UserReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PR#27(Trip 발행 잠금 계약) 연동 — 발행(publish=true)만 {@code lockOwnedTripForPost}를 쓰고
 * DRAFT 저장은 잠금 없는 {@code getOwnedTripForPost}를 쓰는지 확인한다. 실제 잠금 동작(MySQL
 * 동시성)은 Part2의 {@code TripPublicationLockIntegrationTest}(PR#27)에서 이미 검증했으므로,
 * 여기서는 분기 로직만 순수 단위 테스트로 확인한다.
 */
class PostCreateServiceTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private PostImageRepository postImageRepository;
    @Mock
    private PostHashtagRepository postHashtagRepository;
    @Mock
    private PostPlaceRepository postPlaceRepository;
    @Mock
    private TripPostReader tripPostReader;
    @Mock
    private FileLinkService fileLinkService;
    @Mock
    private UserReader userReader;

    private PostCreateService postCreateService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        postCreateService = new PostCreateService(postRepository, postImageRepository, postHashtagRepository,
                postPlaceRepository, tripPostReader, fileLinkService, new PostContentValidator(), userReader);
    }

    private TripPostReader.TripPostSnapshot snapshot() {
        return new TripPostReader.TripPostSnapshot(1L, 10L, "11110", "SOLO", Set.of(), "PUBLIC", null, List.of());
    }

    private CreatePostRequest request(boolean publish) {
        return new CreatePostRequest(1L, "제목", "본문", null, List.of(), List.of(), List.of(),
                PostVisibility.PUBLIC, publish);
    }

    @Test
    void 발행_요청은_lockOwnedTripForPost로_트립을_잠근다() {
        when(tripPostReader.lockOwnedTripForPost(anyLong(), anyLong())).thenReturn(snapshot());

        postCreateService.create(10L, request(true));

        verify(tripPostReader).lockOwnedTripForPost(10L, 1L);
        verify(tripPostReader, never()).getOwnedTripForPost(any(), any());
    }

    @Test
    void DRAFT_저장은_잠금_없는_조회를_쓴다() {
        when(tripPostReader.getOwnedTripForPost(anyLong(), anyLong())).thenReturn(snapshot());

        postCreateService.create(10L, request(false));

        verify(tripPostReader).getOwnedTripForPost(10L, 1L);
        verify(tripPostReader, never()).lockOwnedTripForPost(any(), any());
    }
}
