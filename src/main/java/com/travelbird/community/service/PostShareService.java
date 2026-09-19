package com.travelbird.community.service;

import com.travelbird.community.domain.PostShare;
import com.travelbird.community.domain.ShareChannel;
import com.travelbird.community.repository.PostShareRepository;
import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.post.domain.PostStatus;
import com.travelbird.post.domain.PostVisibility;
import com.travelbird.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 게시글 공유수 증가. backend-functional-spec-v10.md §3.9.5 — 실제 공유 완료 여부와
 * 무관하게 버튼 클릭마다 집계한다. 인증은 선택(로그인 시 {@code post_shares.user_id}
 * 기록, 비로그인은 {@code null}).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PostShareService {

    private static final List<PostVisibility> SHAREABLE_VISIBILITY =
            List.of(PostVisibility.PUBLIC, PostVisibility.MEMO_PRIVATE);

    private final PostRepository postRepository;
    private final PostShareRepository postShareRepository;
    private final ShareValidator shareValidator;

    public void share(Long postId, Long userIdOrNull, String channelRaw) {
        ShareChannel channel = shareValidator.parseChannel(channelRaw);

        postRepository.findByPostIdAndStatusAndVisibilityInAndDeletedAtIsNull(
                        postId, PostStatus.PUBLISHED, SHAREABLE_VISIBILITY)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        // 벌크 업데이트로 증가(낙관적 락 우회) — 이유는 PostRepository.incrementShareCount Javadoc 참고.
        postRepository.incrementShareCount(postId);
        postShareRepository.save(PostShare.of(postId, userIdOrNull, channel));
    }
}
