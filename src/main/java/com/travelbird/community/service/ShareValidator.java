package com.travelbird.community.service;

import com.travelbird.community.domain.ShareChannel;
import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import org.springframework.stereotype.Component;

/** 공유 채널 파싱. backend-functional-spec-v10.md §3.9.5 — 잘못된 채널은 전용 코드로 응답한다. */
@Component
public class ShareValidator {

    public ShareChannel parseChannel(String channelRaw) {
        if (channelRaw == null) {
            throw new BusinessException(ErrorCode.INVALID_SHARE_CHANNEL);
        }
        try {
            return ShareChannel.valueOf(channelRaw);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_SHARE_CHANNEL);
        }
    }
}
