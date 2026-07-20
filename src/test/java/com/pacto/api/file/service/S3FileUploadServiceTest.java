package com.pacto.api.file.service;

import com.pacto.api.file.config.S3Properties;
import com.pacto.api.file.domain.FileCategory;
import com.pacto.api.file.exception.FileUploadException;
import com.pacto.api.file.exception.FileValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3FileUploadServiceTest {

    @Mock
    S3Client s3Client;
    @Mock
    S3Presigner s3Presigner;

    S3FileUploadService fileUploadService;

    @BeforeEach
    void setUp() {
        S3Properties s3Properties = new S3Properties();
        s3Properties.setRegion("ap-northeast-2");
        s3Properties.getS3().setBucket("test-bucket");

        fileUploadService = new S3FileUploadService(s3Client, s3Presigner, s3Properties);
    }

    @Test
    void upload_유효한_이미지는_S3에_저장하고_Object_Key를_반환한다() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "profile.png", "image/png", "dummy-image-bytes".getBytes());
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String objectKey = fileUploadService.upload(FileCategory.PROFILE, 42L, file);

        assertThat(objectKey).matches("profiles/42/[0-9a-fA-F-]{36}\\.png");

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        assertThat(requestCaptor.getValue().bucket()).isEqualTo("test-bucket");
        assertThat(requestCaptor.getValue().key()).isEqualTo(objectKey);
        assertThat(requestCaptor.getValue().contentType()).isEqualTo("image/png");
    }

    @Test
    void upload_유효하지_않은_파일이면_S3를_호출하지_않고_FileValidationException을_던진다() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "malware.exe", "application/octet-stream", "not-an-image".getBytes());

        assertThatThrownBy(() -> fileUploadService.upload(FileCategory.PROFILE, 42L, file))
                .isInstanceOf(FileValidationException.class);

        verifyNoInteractions(s3Client);
    }

    @Test
    void upload_S3_호출이_실패하면_FileUploadException으로_변환한다() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "profile.png", "image/png", "dummy-image-bytes".getBytes());
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkException.create("boom", null));

        assertThatThrownBy(() -> fileUploadService.upload(FileCategory.PROFILE, 42L, file))
                .isInstanceOf(FileUploadException.class)
                .hasCauseInstanceOf(SdkException.class);
    }

    @Test
    void delete_객체를_삭제한다() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        fileUploadService.delete("profiles/42/old.png");

        ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(requestCaptor.capture());
        assertThat(requestCaptor.getValue().bucket()).isEqualTo("test-bucket");
        assertThat(requestCaptor.getValue().key()).isEqualTo("profiles/42/old.png");
    }

    @Test
    void delete_S3_호출이_실패하면_FileUploadException으로_변환한다() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(SdkException.create("boom", null));

        assertThatThrownBy(() -> fileUploadService.delete("profiles/42/old.png"))
                .isInstanceOf(FileUploadException.class)
                .hasCauseInstanceOf(SdkException.class);
    }

    @Test
    void getPresignedUrl_서명된_URL을_반환한다() throws MalformedURLException {
        URL expectedUrl = new URL("https://test-bucket.s3.amazonaws.com/profiles/42/old.png?X-Amz-Signature=abc");
        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(expectedUrl);
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedRequest);

        URL result = fileUploadService.getPresignedUrl("profiles/42/old.png", Duration.ofMinutes(10));

        assertThat(result).isEqualTo(expectedUrl);

        ArgumentCaptor<GetObjectPresignRequest> requestCaptor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(requestCaptor.capture());
        assertThat(requestCaptor.getValue().signatureDuration()).isEqualTo(Duration.ofMinutes(10));
        assertThat(requestCaptor.getValue().getObjectRequest().bucket()).isEqualTo("test-bucket");
        assertThat(requestCaptor.getValue().getObjectRequest().key()).isEqualTo("profiles/42/old.png");
    }

    @Test
    void getPresignedUrl_실패하면_FileUploadException으로_변환한다() {
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenThrow(SdkException.create("boom", null));

        assertThatThrownBy(() -> fileUploadService.getPresignedUrl("profiles/42/old.png", Duration.ofMinutes(10)))
                .isInstanceOf(FileUploadException.class)
                .hasCauseInstanceOf(SdkException.class);
    }
}
