package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.dto.AttachmentDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class AttachmentService {

    @Value("${app.upload.dir:uploads/attachments}")
    private String uploadDir;

    private Path fileStorageLocation;

    @PostConstruct
    public void init() {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
            log.info("Initialized Attachment storage cleanly at {}", this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public Mono<AttachmentDto> storeFile(FilePart filePart) {
        String originalFilename = StringUtils
                .cleanPath(filePart.filename() != null ? filePart.filename() : "unknown_file");

        if (originalFilename.contains("..")) {
            return Mono
                    .error(new RuntimeException("Sorry! Filename contains invalid path sequence " + originalFilename));
        }

        // Generate a unique ID to prevent filename collisions
        String fileId = UUID.randomUUID().toString();
        String extension = "";
        int extIndex = originalFilename.lastIndexOf(".");
        if (extIndex != -1) {
            extension = originalFilename.substring(extIndex);
        }

        String storedFilename = fileId + extension;
        Path targetLocation = this.fileStorageLocation.resolve(storedFilename);

        return filePart.transferTo(targetLocation).then(Mono.fromCallable(() -> {
            String fileDownloadUri = "/api/accounting/attachments/download/" + storedFilename;

            long size = 0;
            try {
                size = Files.size(targetLocation);
            } catch (IOException e) {
                log.warn("Could not determine file size", e);
            }

            String contentType = filePart.headers().getContentType() != null
                    ? filePart.headers().getContentType().toString()
                    : "application/octet-stream";

            return AttachmentDto.builder()
                    .id(storedFilename) // Use storedFilename as the ID
                    .originalFilename(originalFilename)
                    .contentType(contentType)
                    .size(size)
                    .url(fileDownloadUri)
                    .uploadedAt(LocalDateTime.now())
                    .build();
        }));
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File not found " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File not found " + fileName, ex);
        }
    }
}
