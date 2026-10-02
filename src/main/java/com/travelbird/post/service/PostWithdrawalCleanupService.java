package com.travelbird.post.service;

import com.travelbird.ai.api.AiPreviewWithdrawalCleanup;
import com.travelbird.community.repository.PostShareRepository;
import com.travelbird.community.repository.PostViewHistoryRepository;
import com.travelbird.community.repository.ReportRepository;
import com.travelbird.place.repository.SavedPlaceRepository;
import com.travelbird.post.api.PostWithdrawalCleanup;
import com.travelbird.post.domain.Post;
import com.travelbird.post.repository.PostHashtagRepository;
import com.travelbird.post.repository.PostImageRepository;
import com.travelbird.post.repository.PostPlaceRepository;
import com.travelbird.post.repository.PostRepository;
import com.travelbird.savedroute.domain.SavedRoute;
import com.travelbird.savedroute.domain.SavedRouteSourceType;
import com.travelbird.savedroute.repository.SavedRouteRepository;
import com.travelbird.trip.api.TripPostReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 회원 탈퇴 2단계(콘텐츠 정리). backend-functional-spec-v10.md §3.1.4 — Part2 Trip 삭제(3단계)
 * 보다 반드시 먼저 실행해야 한다.
 *
 * <p>일반 삭제({@code PostDeleteService})와 달리 tombstone이 아니라 <b>물리 삭제</b>한다 —
 * {@code posts.trip_id}가 {@code ON DELETE RESTRICT}라 행이 남아 있으면 Part2의 Trip 삭제가
 * 실패하고, {@code post_places.trip_place_id}/{@code post_images.file_id}도 RESTRICT라 각각
 * Trip 삭제와 Part1 4단계 파일 삭제를 막는다. 이미 삭제(tombstone)된 Post도 {@code trip_id}를
 * 유지하므로 삭제 여부와 무관하게 사용자의 모든 Post 행을 대상으로 한다.
 *
 * <p>이미지 파일(S3·DB)은 Part1 4단계가 사용자 파일 전체를 지우므로 여기서는 연결 행만
 * 지운다 — 중복 S3 삭제를 피한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PostWithdrawalCleanupService implements PostWithdrawalCleanup {

    private final TripPostReader tripPostReader;
    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final PostPlaceRepository postPlaceRepository;
    private final PostHashtagRepository postHashtagRepository;
    private final SavedRouteRepository savedRouteRepository;
    private final SavedPlaceRepository savedPlaceRepository;
    private final ReportRepository reportRepository;
    private final PostViewHistoryRepository postViewHistoryRepository;
    private final PostShareRepository postShareRepository;
    private final AiPreviewWithdrawalCleanup aiPreviewWithdrawalCleanup;

    @Override
    public void cleanupUserContent(Long userId) {
        deleteOwnedPosts(userId);
        deleteOwnedSavedRoutes(userId);
        savedPlaceRepository.deleteByIdUserId(userId);
        reportRepository.deleteByReporterUserId(userId);
        postViewHistoryRepository.deleteByViewerUserId(userId);
        postShareRepository.clearUser(userId);
    }

    private void deleteOwnedPosts(Long userId) {
        List<Long> tripIds = tripPostReader.getTripIdsByUser(userId);
        if (tripIds.isEmpty()) {
            return;
        }
        List<Post> posts = postRepository.findByTripIdIn(tripIds);
        if (posts.isEmpty()) {
            return;
        }
        List<Long> postIds = posts.stream().map(Post::getPostId).toList();

        // 타 사용자의 저장 관계는 지우지 않고 접근 불가로만 표시한다(§3.1.4).
        savedRouteRepository.markUnavailableBySourceIds(SavedRouteSourceType.POST, postIds);

        // 자식 행은 DB CASCADE에만 맡기지 않고 먼저 명시 삭제 후 flush한다 — Hibernate 기본 flush
        // 순서(insert/update → delete)에 기대지 않기 위해 PostDeleteService와 같은 방식을 쓴다.
        for (Long postId : postIds) {
            postImageRepository.deleteByIdPostId(postId);
            postPlaceRepository.deleteByIdPostId(postId);
            postHashtagRepository.deleteByIdPostId(postId);
        }
        postRepository.flush();
        postRepository.deleteAll(posts);
        postRepository.flush();
    }

    /**
     * 본인이 저장한 경로를 전부 지운다. POST 원본은 {@code saveCount}를 되돌리고(저장 취소와 동일
     * 규칙), AI_PREVIEW는 Part2 계약대로 previewId만 넘겨 강등 판단을 맡긴다 — 대화형 취소
     * 흐름({@code AiPreviewSavedRouteService.cancel})은 SavedRoute 삭제까지 스스로 하므로
     * 여기서 재사용하지 않는다(중복 삭제).
     */
    private void deleteOwnedSavedRoutes(Long userId) {
        List<SavedRoute> routes = savedRouteRepository.findByUserId(userId);
        if (routes.isEmpty()) {
            return;
        }
        for (SavedRoute route : routes) {
            if (route.getSourceType() == SavedRouteSourceType.POST && route.isSourceAvailable()) {
                postRepository.decrementSaveCount(route.getSourceId());
            }
        }
        List<Long> previewIds = routes.stream()
                .filter(route -> route.getSourceType() == SavedRouteSourceType.AI_PREVIEW)
                .map(SavedRoute::getSourceId)
                .toList();

        savedRouteRepository.deleteAll(routes);
        savedRouteRepository.flush();

        if (!previewIds.isEmpty()) {
            aiPreviewWithdrawalCleanup.demoteUnreferencedPreviews(previewIds);
        }
    }
}
