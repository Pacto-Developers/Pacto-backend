package com.pacto.api.file.service;

import com.pacto.api.file.exception.FileValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileValidatorTest {

    @Test
    void 유효한_이미지는_예외를_던지지_않는다() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.png", "image/png", "dummy-image-bytes".getBytes());

        assertThatCode(() -> FileValidator.validateImage(file)).doesNotThrowAnyException();
    }

    @Test
    void 확장자와_컨텐츠타입이_대문자여도_통과한다() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "PHOTO.PNG", "IMAGE/PNG", "dummy-image-bytes".getBytes());

        assertThatCode(() -> FileValidator.validateImage(file)).doesNotThrowAnyException();
    }

    @Test
    void 파일이_null이면_예외를_던진다() {
        assertThatThrownBy(() -> FileValidator.validateImage(null))
                .isInstanceOf(FileValidationException.class)
                .hasMessage("업로드할 파일이 없습니다.");
    }

    @Test
    void 빈_파일이면_예외를_던진다() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> FileValidator.validateImage(file))
                .isInstanceOf(FileValidationException.class)
                .hasMessage("업로드할 파일이 없습니다.");
    }

    @Test
    void 파일_크기가_10MB를_초과하면_예외를_던진다() {
        byte[] tooLarge = new byte[10 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", tooLarge);

        assertThatThrownBy(() -> FileValidator.validateImage(file))
                .isInstanceOf(FileValidationException.class)
                .hasMessage("파일 크기는 10MB를 초과할 수 없습니다.");
    }

    @Test
    void 컨텐츠타입이_null이면_예외를_던진다() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", null, "dummy".getBytes());

        assertThatThrownBy(() -> FileValidator.validateImage(file))
                .isInstanceOf(FileValidationException.class)
                .hasMessageContaining("지원하지 않는 파일 형식입니다");
    }

    @Test
    void 허용되지_않은_컨텐츠타입이면_예외를_던진다() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", "dummy".getBytes());

        assertThatThrownBy(() -> FileValidator.validateImage(file))
                .isInstanceOf(FileValidationException.class)
                .hasMessageContaining("지원하지 않는 파일 형식입니다");
    }

    @Test
    void 컨텐츠타입은_허용되지만_확장자가_허용되지_않으면_예외를_던진다() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.bmp", "image/png", "dummy-image-bytes".getBytes());

        assertThatThrownBy(() -> FileValidator.validateImage(file))
                .isInstanceOf(FileValidationException.class)
                .hasMessageContaining("지원하지 않는 파일 확장자입니다");
    }

    @Test
    void 확장자가_없으면_예외를_던진다() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo", "image/png", "dummy-image-bytes".getBytes());

        assertThatThrownBy(() -> FileValidator.validateImage(file))
                .isInstanceOf(FileValidationException.class)
                .hasMessageContaining("지원하지 않는 파일 확장자입니다");
    }
}
