package com.pacto.api.file.service;

import com.pacto.api.file.exception.FileValidationException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

public class FileValidator {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp", "gif"
    );
    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

    private FileValidator() {
    }

    public static void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileValidationException("업로드할 파일이 없습니다.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new FileValidationException("파일 크기는 10MB를 초과할 수 없습니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new FileValidationException("지원하지 않는 파일 형식입니다. (jpg, png, webp, gif만 허용)");
        }

        String extension = FileExtensions.extract(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new FileValidationException("지원하지 않는 파일 확장자입니다. (jpg, png, webp, gif만 허용)");
        }
    }
}
