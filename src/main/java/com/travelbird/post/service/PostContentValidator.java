package com.travelbird.post.service;

import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 게시글 작성/수정 공통 검증. backend-functional-spec-v10.md §3.8.1 — 제목·본문·해시태그
 * 길이/개수 제한은 DRAFT·발행 요청 모두에 적용하고, "필수 여부"만 {@code publish} 값에
 * 따라 갈린다.
 */
@Component
public class PostContentValidator {

    private static final int TITLE_MAX_LENGTH = 50;
    private static final int CONTENT_MAX_LENGTH = 2000;
    private static final int HASHTAG_MAX_COUNT = 5;
    private static final int HASHTAG_MAX_LENGTH = 10;
    private static final int IMAGE_MAX_COUNT = 10;

    public void validateTitle(String title) {
        if (title != null && title.length() > TITLE_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.POST_TITLE_TOO_LONG);
        }
    }

    public void validateContent(String content) {
        if (content != null && content.length() > CONTENT_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.POST_CONTENT_TOO_LONG);
        }
    }

    public void validateHashtags(List<String> hashtags) {
        if (hashtags == null) {
            return;
        }
        if (hashtags.size() > HASHTAG_MAX_COUNT) {
            throw new BusinessException(ErrorCode.POST_HASHTAG_LIMIT_EXCEEDED);
        }
        for (String hashtag : hashtags) {
            if (hashtag != null && hashtag.length() > HASHTAG_MAX_LENGTH) {
                throw new BusinessException(ErrorCode.POST_HASHTAG_TOO_LONG);
            }
        }
    }

    public void validateImageCount(List<Long> imageFileIds) {
        if (imageFileIds != null && imageFileIds.size() > IMAGE_MAX_COUNT) {
            throw new BusinessException(ErrorCode.POST_IMAGE_LIMIT_EXCEEDED);
        }
    }

    /**
     * {@code imageFileIds}에 같은 fileId가 중복되면 거부한다. {@code FileLinkService.
     * validateLinkableFiles}는 PR#24부터 중복 fileId를 통과시키는데, {@code post_images}는
     * {@code PRIMARY KEY(post_id, file_id)} + {@code UNIQUE(file_id)}라 중복이 그대로
     * 저장 단계까지 가면 처리되지 않은 {@code DataIntegrityViolationException}으로 샌다 —
     * 저장 전에 명확한 400으로 막는다.
     */
    public void validateNoDuplicateImages(List<Long> imageFileIds) {
        if (imageFileIds == null) {
            return;
        }
        if (imageFileIds.stream().distinct().count() != imageFileIds.size()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    /** {@code publish=true}면 제목·본문이 공백이 아니어야 한다. */
    public void validateRequiredForPublish(String title, String content, boolean publish) {
        if (publish && (!StringUtils.hasText(title) || !StringUtils.hasText(content))) {
            throw new BusinessException(ErrorCode.INVALID_POST_CONTENT);
        }
    }
}
