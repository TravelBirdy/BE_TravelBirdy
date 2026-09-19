package com.travelbird.post.service;

import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostContentValidatorTest {

    private final PostContentValidator validator = new PostContentValidator();

    @Test
    void 제목_50자는_통과한다() {
        validator.validateTitle("가".repeat(50));
    }

    @Test
    void 제목_51자는_실패한다() {
        assertThatThrownBy(() -> validator.validateTitle("가".repeat(51)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.POST_TITLE_TOO_LONG);
    }

    @Test
    void 본문_2000자는_통과하고_2001자는_실패한다() {
        validator.validateContent("가".repeat(2000));
        assertThatThrownBy(() -> validator.validateContent("가".repeat(2001)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.POST_CONTENT_TOO_LONG);
    }

    @Test
    void 해시태그_5개는_통과하고_6개는_실패한다() {
        validator.validateHashtags(List.of("a", "b", "c", "d", "e"));
        assertThatThrownBy(() -> validator.validateHashtags(List.of("a", "b", "c", "d", "e", "f")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.POST_HASHTAG_LIMIT_EXCEEDED);
    }

    @Test
    void 해시태그_10자는_통과하고_11자는_실패한다() {
        validator.validateHashtags(List.of("가".repeat(10)));
        assertThatThrownBy(() -> validator.validateHashtags(List.of("가".repeat(11))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.POST_HASHTAG_TOO_LONG);
    }

    @Test
    void 이미지_10장은_통과하고_11장은_실패한다() {
        validator.validateImageCount(List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L));
        assertThatThrownBy(() -> validator.validateImageCount(List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.POST_IMAGE_LIMIT_EXCEEDED);
    }

    @Test
    void publish_true면_제목과_본문이_필수다() {
        assertThatThrownBy(() -> validator.validateRequiredForPublish(null, "본문", true))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_POST_CONTENT);
        assertThatThrownBy(() -> validator.validateRequiredForPublish("제목", " ", true))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_POST_CONTENT);
    }

    @Test
    void publish_false면_제목과_본문이_없어도_된다() {
        validator.validateRequiredForPublish(null, null, false);
    }

    @Test
    void null이나_빈_목록은_카운트_검증을_통과한다() {
        validator.validateHashtags(null);
        validator.validateImageCount(null);
        validator.validateHashtags(List.of());
        validator.validateImageCount(List.of());
        assertThat(true).isTrue();
    }
}
