package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.common.dto.ApiResponseWrapper;
import com.yowyob.erp.accounting.dto.AttachmentDto;
import com.yowyob.erp.accounting.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;

@RestController
@RequestMapping("/api/accounting/attachments")
@RequiredArgsConstructor
@Tag(name = "Attachment Management", description = "Generic endpoints for uploading and downloading files")
@Slf4j
public class AttachmentController {

    private final AttachmentService attachmentService;

    @Operation(summary = "Upload a generic file attachment")
    @PostMapping(value = "/upload", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public Mono<ResponseEntity<ApiResponseWrapper<AttachmentDto>>> uploadAttachment(
            @RequestPart("file") FilePart file) {
        log.info("Uploading attachment: {}", file.filename());
        return attachmentService.storeFile(file)
                .map(dto -> ResponseEntity.ok(ApiResponseWrapper.success(dto, "File successfully uploaded")));
    }

    @Operation(summary = "Download or view a generic file attachment")
    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable String fileName) {
        Resource resource = attachmentService.loadFileAsResource(fileName);

        String contentType = null;
        try {
            contentType = Files.probeContentType(resource.getFile().toPath());
        } catch (IOException ex) {
            log.info("Could not determine file type.");
        }

        // Fallback to the default content type if type could not be determined
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
