package com.aivideo.ai;

import lombok.Getter;

@Getter
public class AiException extends RuntimeException {

    private final String code;

    public AiException(String code, String message) {
        super(message);
        this.code = code;
    }

    public AiException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
