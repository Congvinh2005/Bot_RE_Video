package com.aivideo.voice;

import lombok.Getter;

@Getter
public class VoiceException extends RuntimeException {

    private final String code;

    public VoiceException(String code, String message) {
        super(message);
        this.code = code;
    }

    public VoiceException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
