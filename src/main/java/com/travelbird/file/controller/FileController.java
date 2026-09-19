package com.travelbird.file.controller;

import com.travelbird.file.dto.request.PresignedUploadRequest;
import com.travelbird.file.dto.response.PresignedUploadResponse;
import com.travelbird.file.service.FileUploadService;
import com.travelbird.global.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FileController {

    private final FileUploadService fileUploadService;

    public FileController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @PostMapping("/api/files/presigned-uploads")
    public PresignedUploadResponse createPresignedUpload(@RequestBody PresignedUploadRequest request) {
        return fileUploadService.createPresignedUpload(SecurityUtils.getCurrentUserId(), request);
    }

    @PostMapping("/api/files/{fileId}/complete")
    public ResponseEntity<Void> completeUpload(@PathVariable Long fileId) {
        fileUploadService.completeUpload(SecurityUtils.getCurrentUserId(), fileId);
        return ResponseEntity.noContent().build();
    }
}
