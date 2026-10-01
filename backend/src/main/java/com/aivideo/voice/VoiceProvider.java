package com.aivideo.voice;

/** Abstraction TTS. Thay provider chỉ cần thêm class implement interface này. */
public interface VoiceProvider {

    /** Tên voice mà provider này phục vụ (vd "ADAM"). */
    String voiceName();

    /** Sinh audio bytes từ text. Lỗi -> VoiceException. */
    byte[] generateSpeech(String text, VoiceOptions options);
}
