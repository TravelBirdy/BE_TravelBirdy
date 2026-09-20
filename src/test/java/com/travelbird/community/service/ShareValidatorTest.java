package com.travelbird.community.service;

import com.travelbird.community.domain.ShareChannel;
import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShareValidatorTest {

    private final ShareValidator validator = new ShareValidator();

    @Test
    void 유효한_채널은_파싱된다() {
        assertThat(validator.parseChannel("KAKAO")).isEqualTo(ShareChannel.KAKAO);
        assertThat(validator.parseChannel("LINK")).isEqualTo(ShareChannel.LINK);
        assertThat(validator.parseChannel("OTHER")).isEqualTo(ShareChannel.OTHER);
    }

    @Test
    void 잘못된_채널은_예외를_던진다() {
        assertThatThrownBy(() -> validator.parseChannel("INSTAGRAM"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_SHARE_CHANNEL));
    }

    @Test
    void null_채널은_예외를_던진다() {
        assertThatThrownBy(() -> validator.parseChannel(null))
                .isInstanceOf(BusinessException.class);
    }
}
