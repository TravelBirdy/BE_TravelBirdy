package com.travelbird.post.service;

import com.travelbird.post.api.CommunityPostCard;
import com.travelbird.post.api.HomePostReader;
import com.travelbird.post.domain.Post;
import com.travelbird.post.domain.PostStatus;
import com.travelbird.post.domain.PostVisibility;
import com.travelbird.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@code /api/home} 추천 기록. backend-functional-spec-v10.md "추천 기록은 커뮤니티 인기
 * 점수와 랜덤 셔플을 혼합한다" — 인기 점수(조회수×1+저장수×3+공유수×5) 상위 후보군을 뽑아
 * 셔플 후 {@code limit}개를 취한다. 카드 변환은 {@link CommunityPostCardAssembler}
 * (커뮤니티 목록/검색과 공유) 참고 — {@code region}/{@code companionType}/{@code themes}/
 * {@code author} placeholder 사유도 거기 Javadoc에 있다.
 */
@Service
@RequiredArgsConstructor
public class HomePostReaderImpl implements HomePostReader {

    private final PostRepository postRepository;
    private final CommunityPostCardAssembler cardAssembler;

    @Override
    public List<CommunityPostCard> getHomeRecommendedPosts(int limit, Long viewerIdOrNull) {
        if (limit <= 0) {
            return List.of();
        }
        List<Post> candidates = postRepository.findHomeCandidates(
                PostStatus.PUBLISHED,
                List.of(PostVisibility.PUBLIC, PostVisibility.MEMO_PRIVATE),
                PageRequest.of(0, Math.max(limit * 5, 50)));

        List<Post> shuffled = new ArrayList<>(candidates);
        Collections.shuffle(shuffled);
        List<Post> picked = shuffled.stream().limit(limit).toList();

        return cardAssembler.toCards(picked, viewerIdOrNull);
    }
}
