package com.travelbird.post.service;

import com.travelbird.file.api.FileLinkService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 게시글 수정 커밋 이후 제거된 이미지 파일(S3·DB 행)을 삭제한다. 롤백되면 이 리스너는 호출되지
 * 않으므로 실패한 수정이 S3 이미지를 지우지 않는다. AFTER_COMMIT에서 DB를 쓰려면 새 트랜잭션이
 * 필요해 {@code REQUIRES_NEW}를 쓴다. 삭제 실패는 이미 커밋된 수정 응답을 바꾸지 않도록 로그만
 * 남긴다(파일 행이 LINKED 상태로 남을 뿐이다).
 */
@Component
@RequiredArgsConstructor
public class PostImageCleanupListener {

    private static final Logger log = LoggerFactory.getLogger(PostImageCleanupListener.class);

    private final FileLinkService fileLinkService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onImagesRemoved(PostImagesRemovedEvent event) {
        try {
            fileLinkService.deleteOwnedFiles(event.userId(), event.fileIds());
        } catch (RuntimeException e) {
            log.warn("게시글 수정 후 제거된 이미지 파일 삭제 실패. userId={}, fileIds={}",
                    event.userId(), event.fileIds(), e);
        }
    }
}
