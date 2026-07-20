package com.pacto.api.file.exception;

public class FileUploadException extends RuntimeException {
    public FileUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
