package com.travelbird.mypage.dto.response;

public record MyPageResponse(
        MyPageProfile profile,
        MyPageStatistics statistics
) {
}
