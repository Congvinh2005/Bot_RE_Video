package com.aivideo.video;

import lombok.Getter;

@Getter
public class SubtitleException extends RuntimeException {

    private final String code;

    public SubtitleException(String code, String message) {
        super(message);
        this.code = code;
    }

    public SubtitleException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
