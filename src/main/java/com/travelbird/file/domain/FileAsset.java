package com.travelbird.file.domain;

import com.travelbird.user.domain.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "files")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class FileAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long fileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "object_key", nullable = false, unique = true)
    private String objectKey;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "width", nullable = false)
    private Integer width;

    @Column(name = "height", nullable = false)
    private Integer height;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false)
    private FilePurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FileStatus status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private FileAsset(User user, String fileName, String objectKey, String contentType, Long sizeBytes,
                       Integer width, Integer height, FilePurpose purpose, LocalDateTime expiresAt) {
        this.user = user;
        this.fileName = fileName;
        this.objectKey = objectKey;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.width = width;
        this.height = height;
        this.purpose = purpose;
        this.status = FileStatus.PENDING;
        this.expiresAt = expiresAt;
    }

    /**
     * Presigned URL 발급 시점에 {@link FileStatus#PENDING}으로 생성한다 (기능명세서 3.5.1).
     */
    public static FileAsset createPending(User user, String fileName, String objectKey, String contentType,
                                           Long sizeBytes, Integer width, Integer height, FilePurpose purpose,
                                           LocalDateTime expiresAt) {
        return new FileAsset(user, fileName, objectKey, contentType, sizeBytes, width, height, purpose, expiresAt);
    }

    public boolean isOwnedBy(Long userId) {
        return user.getUserId().equals(userId);
    }

    /**
     * S3 실제 객체 검증(MIME/시그니처/용량/가로크기) 통과 후 호출한다. 이미 UPLOADED/LINKED면
     * 아무것도 안 하는 멱등 처리(호출부에서 상태를 보고 판단).
     */
    public void markUploaded() {
        this.status = FileStatus.UPLOADED;
    }

    public void markLinked() {
        this.status = FileStatus.LINKED;
    }
}
