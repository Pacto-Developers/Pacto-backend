package com.pacto.api.file.service;

import com.pacto.api.file.config.S3Properties;
import com.pacto.api.file.domain.FileCategory;
import com.pacto.api.file.exception.FileUploadException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class S3FileUploadService implements FileUploadService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;

    @Override
    public String upload(FileCategory category, Long ownerId, MultipartFile file) {
        FileValidator.validateImage(file);
        String objectKey = ObjectKeyGenerator.generate(category, ownerId, file.getOriginalFilename());
        String bucket = s3Properties.getS3().getBucket();

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(objectKey)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (IOException | SdkException e) {
            throw new FileUploadException("파일 업로드에 실패했습니다.", e);
        }

        return objectKey;
    }

    @Override
    public void delete(String objectKey) {
        String bucket = s3Properties.getS3().getBucket();
        try {
            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(bucket)
                            .key(objectKey)
                            .build()
            );
        } catch (SdkException e) {
            throw new FileUploadException("파일 삭제에 실패했습니다.", e);
        }
    }

    @Override
    public URL getPresignedUrl(String objectKey, Duration expiration) {
        String bucket = s3Properties.getS3().getBucket();
        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .getObjectRequest(
                            GetObjectRequest.builder()
                                    .bucket(bucket)
                                    .key(objectKey)
                                    .build()
                    )
                    .build();
            return s3Presigner.presignGetObject(presignRequest).url();
        } catch (SdkException e) {
            throw new FileUploadException("Presigned URL 생성에 실패했습니다.", e);
        }
    }
}
