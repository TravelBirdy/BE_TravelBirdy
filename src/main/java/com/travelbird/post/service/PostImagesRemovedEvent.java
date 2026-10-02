package com.travelbird.post.service;

import java.util.List;

/**
 * 게시글 수정으로 첨부에서 빠진 이미지. 파일(S3·DB) 삭제는 수정 트랜잭션이 커밋된 뒤
 * {@link PostImageCleanupListener}가 처리한다 — 트랜잭션 안에서 S3를 먼저 지우면 이후 검증
 * 실패로 DB는 롤백돼도 S3 객체는 복구되지 않는다.
 */
public record PostImagesRemovedEvent(Long userId, List<Long> fileIds) {
}
