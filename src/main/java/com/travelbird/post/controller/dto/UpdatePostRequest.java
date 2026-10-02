package com.travelbird.post.controller.dto;

import com.travelbird.post.domain.PostVisibility;

import java.util.List;

/**
 * backend-functional-spec-v10.md §3.8.4. PATCH는 "필드 미전달(기존값 유지)"과
 * "명시적 {@code null}(값 삭제)"을 구분해야 한다(문서 78번째 줄 전역 규칙) — 이 코드베이스는
 * record 대신 mutable 클래스 + {@code xxxPresent} 플래그로 이 문제를 이미 풀어두었다
 * ({@code trip.dto.request.UpdateTripRequest} 참고). Jackson은 JSON 키가 존재하면 값이
 * {@code null}이어도 setter를 호출하므로, setter에서 플래그를 같이 세팅하면 "키 존재 여부"를
 * 그대로 잡아낼 수 있다.
 *
 * <p>{@code imageFileIds}/{@code placeIds}/{@code hashtags}는 "전달 시 non-null"이라
 * {@code []}이 곧 "비우기"를 의미하므로 값 자체로 삭제를 표현할 수 있다 — 그래도 "미전달(변경
 * 없음)"과 구분은 필요해서 presence 플래그를 둔다. {@code publish}는 단순 선택 필드라
 * {@code null}/{@code false}/미전달을 전부 "전환 없음"으로 동일하게 취급하므로 플래그가
 * 필요 없다. {@code version}은 항상 필수다.
 */
public class UpdatePostRequest {

    private String title;
    private boolean titlePresent;

    private String content;
    private boolean contentPresent;

    private Long representativeFileId;
    private boolean representativeFileIdPresent;

    private List<Long> imageFileIds;
    private boolean imageFileIdsPresent;

    private List<Long> placeIds;
    private boolean placeIdsPresent;

    private List<String> hashtags;
    private boolean hashtagsPresent;

    private PostVisibility visibility;
    private boolean visibilityPresent;

    private Boolean publish;

    private Long version;

    public String title() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        this.titlePresent = true;
    }

    public boolean titlePresent() {
        return titlePresent;
    }

    public String content() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        this.contentPresent = true;
    }

    public boolean contentPresent() {
        return contentPresent;
    }

    public Long representativeFileId() {
        return representativeFileId;
    }

    public void setRepresentativeFileId(Long representativeFileId) {
        this.representativeFileId = representativeFileId;
        this.representativeFileIdPresent = true;
    }

    public boolean representativeFileIdPresent() {
        return representativeFileIdPresent;
    }

    public List<Long> imageFileIds() {
        return imageFileIds;
    }

    public void setImageFileIds(List<Long> imageFileIds) {
        this.imageFileIds = imageFileIds;
        this.imageFileIdsPresent = true;
    }

    public boolean imageFileIdsPresent() {
        return imageFileIdsPresent;
    }

    public List<Long> placeIds() {
        return placeIds;
    }

    public void setPlaceIds(List<Long> placeIds) {
        this.placeIds = placeIds;
        this.placeIdsPresent = true;
    }

    public boolean placeIdsPresent() {
        return placeIdsPresent;
    }

    public List<String> hashtags() {
        return hashtags;
    }

    public void setHashtags(List<String> hashtags) {
        this.hashtags = hashtags;
        this.hashtagsPresent = true;
    }

    public boolean hashtagsPresent() {
        return hashtagsPresent;
    }

    public PostVisibility visibility() {
        return visibility;
    }

    public void setVisibility(PostVisibility visibility) {
        this.visibility = visibility;
        this.visibilityPresent = true;
    }

    public boolean visibilityPresent() {
        return visibilityPresent;
    }

    public Boolean publish() {
        return publish;
    }

    public void setPublish(Boolean publish) {
        this.publish = publish;
    }

    public Long version() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
