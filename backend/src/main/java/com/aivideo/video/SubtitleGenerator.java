package com.aivideo.video;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Chia script thành cue SRT, thời lượng mỗi cue tỉ lệ theo số ký tự để
 * đồng bộ với audio. Không phụ thuộc timestamp word-level của STT/TTS.
 */
@Component
@RequiredArgsConstructor
public class SubtitleGenerator {

    private final SubtitleProperties properties;

    public record Cue(double start, double end, String text) {
    }

    public String generate(String script, double audioDurationSeconds) {
        List<Cue> cues = distribute(script, audioDurationSeconds);
        StringBuilder srt = new StringBuilder();
        for (int i = 0; i < cues.size(); i++) {
            Cue cue = cues.get(i);
            srt.append(i + 1).append('\n');
            srt.append(formatTimestamp(cue.start())).append(" --> ")
                    .append(formatTimestamp(cue.end())).append('\n');
            srt.append(cue.text()).append("\n\n");
        }
        return srt.toString();
    }

    List<Cue> distribute(String script, double duration) {
        if (script == null || script.isBlank()) {
            throw new SubtitleException("SUBTITLE_FAILED", "Script is empty");
        }
        if (duration <= 0) {
            throw new SubtitleException("SUBTITLE_FAILED", "Audio duration must be positive");
        }
        List<String> sentences = splitSentences(script.strip());
        int maxCueChars = properties.maxCharsPerLine() * 2;
        List<String> cueTexts = pack(sentences, maxCueChars);
        int totalChars = cueTexts.stream().mapToInt(String::length).sum();

        List<Cue> cues = new ArrayList<>();
        double cursor = 0;
        for (int i = 0; i < cueTexts.size(); i++) {
            double start = cursor;
            double end = i == cueTexts.size() - 1
                    ? duration
                    : cursor + duration * cueTexts.get(i).length() / totalChars;
            cues.add(new Cue(start, end, wrap(cueTexts.get(i))));
            cursor = end;
        }
        return cues;
    }

    private List<String> splitSentences(String script) {
        List<String> sentences = new ArrayList<>();
        for (String line : script.split("\\n")) {
            for (String part : line.split("(?<=[.!?…])\\s+")) {
                String sentence = part.strip();
                if (!sentence.isEmpty()) {
                    sentences.add(sentence);
                }
            }
        }
        return sentences;
    }

    private List<String> pack(List<String> sentences, int maxCueChars) {
        List<String> cues = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String sentence : sentences) {
            if (sentence.length() > maxCueChars) {
                if (current.length() > 0) {
                    cues.add(current.toString());
                    current.setLength(0);
                }
                cues.addAll(splitLong(sentence, maxCueChars));
            } else if (current.length() + 1 + sentence.length() <= maxCueChars) {
                if (current.length() > 0) {
                    current.append(' ');
                }
                current.append(sentence);
            } else {
                cues.add(current.toString());
                current.setLength(0);
                current.append(sentence);
            }
        }
        if (current.length() > 0) {
            cues.add(current.toString());
        }
        return cues;
    }

    private List<String> splitLong(String sentence, int maxCueChars) {
        List<String> parts = new ArrayList<>();
        String[] words = sentence.split("\\s+");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            if (current.length() + 1 + word.length() > maxCueChars && current.length() > 0) {
                parts.add(current.toString());
                current.setLength(0);
            }
            if (current.length() > 0) {
                current.append(' ');
            }
            current.append(word);
        }
        if (current.length() > 0) {
            parts.add(current.toString());
        }
        return parts;
    }

    private String wrap(String cueText) {
        int max = properties.maxCharsPerLine();
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : cueText.split("\\s+")) {
            if (current.length() + 1 + word.length() > max && current.length() > 0) {
                lines.add(current.toString());
                current.setLength(0);
            }
            if (current.length() > 0) {
                current.append(' ');
            }
            current.append(word);
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return String.join("\n", lines);
    }

    static String formatTimestamp(double seconds) {
        if (seconds < 0) {
            seconds = 0;
        }
        long totalMillis = Math.round(seconds * 1000);
        long hours = totalMillis / 3_600_000;
        long minutes = (totalMillis % 3_600_000) / 60_000;
        long secs = (totalMillis % 60_000) / 1000;
        long millis = totalMillis % 1000;
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, secs, millis);
    }
}
