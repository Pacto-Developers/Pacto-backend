package com.pacto.api.file.service;

import com.pacto.api.file.domain.FileCategory;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.time.Duration;

public interface FileUploadService {

    String upload(FileCategory category, Long ownerId, MultipartFile file);

    void delete(String objectKey);

    URL getPresignedUrl(String objectKey, Duration expiration);
}
