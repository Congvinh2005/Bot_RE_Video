package com.aivideo.source;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class VideoUploadException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public VideoUploadException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public VideoUploadException(String code, String message, Throwable cause, HttpStatus status) {
        super(message, cause);
        this.code = code;
        this.status = status;
    }
}
