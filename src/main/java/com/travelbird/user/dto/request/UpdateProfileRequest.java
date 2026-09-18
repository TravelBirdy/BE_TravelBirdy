package com.travelbird.user.dto.request;

/**
 * PATCH 요청 body에서 필드 미전달(유지)과 명시적 null(지우기)을 구분하기 위해,
 * 각 필드마다 "전달 여부"를 별도로 들고 있다. 컨트롤러가 JsonNode로 원시 body를
 * 파싱해서 이 record를 만든다({@code has("nickname")} 등으로 전달 여부 판단).
 */
public record UpdateProfileRequest(
        boolean nicknamePresent,
        String nickname,
        boolean introductionPresent,
        String introduction
) {
}
