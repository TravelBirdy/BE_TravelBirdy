package com.travelbird.search.service;

import com.travelbird.community.repository.CommunityPostSearchRepository;
import com.travelbird.community.service.CommunityBlockFilter;
import com.travelbird.post.api.CommunityPostCard;
import com.travelbird.post.domain.Post;
import com.travelbird.post.service.CommunityPostCardAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 통합검색(§3.16.1) 기록 섹션. 커뮤니티 검색과 같은 저장소·차단 필터·카드 조립기를 그대로 써서
 * {@code PUBLISHED}, {@code PUBLIC|MEMO_PRIVATE}, 비삭제 게시글만 나온다. 첫 페이지만 필요해서
 * 커서 처리는 하지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostSearchSectionService {

    private final CommunityPostSearchRepository searchRepository;
    private final CommunityBlockFilter blockFilter;
    private final CommunityPostCardAssembler cardAssembler;

    public List<CommunityPostCard> search(String query, Long viewerId, int limit) {
        List<Long> excludedTripIds = blockFilter.resolveExcludedTripIds(viewerId);
        List<Post> posts = searchRepository.searchFirstPage(query, excludedTripIds, PageRequest.of(0, limit));
        return cardAssembler.toCards(posts, viewerId);
    }
}
