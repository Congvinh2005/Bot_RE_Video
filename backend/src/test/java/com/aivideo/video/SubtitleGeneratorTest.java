package com.aivideo.video;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubtitleGeneratorTest {

    private final SubtitleGenerator generator = new SubtitleGenerator(new SubtitleProperties(42));

    private static final Pattern CUE_HEADER =
            Pattern.compile("(?m)^\\d+\\n\\d{2}:\\d{2}:\\d{2},\\d{3} --> \\d{2}:\\d{2}:\\d{2},\\d{3}$");

    @Test
    void generateProducesValidSrtSyncedToDuration() {
        String script = "Bo ngu mem mat cho nang. Chat lieu thun lanh, mac mua he rat thoai mai! Mua ngay hom nay.";

        String srt = generator.generate(script, 10.0);

        assertThat(CUE_HEADER.matcher(srt).results().count()).isGreaterThan(0);
        List<SubtitleGenerator.Cue> cues = generator.distribute(script, 10.0);
        assertThat(cues.get(0).start()).isEqualTo(0.0);
        assertThat(cues.get(cues.size() - 1).end()).isEqualTo(10.0);
        for (int i = 1; i < cues.size(); i++) {
            assertThat(cues.get(i).start())
                    .isGreaterThanOrEqualTo(cues.get(i - 1).end() - 0.001);
            assertThat(cues.get(i).end()).isGreaterThan(cues.get(i).start());
        }
        for (SubtitleGenerator.Cue cue : cues) {
            for (String line : cue.text().split("\n")) {
                assertThat(line.length()).isLessThanOrEqualTo(42);
            }
        }
    }

    @Test
    void distributeKeepsAllWords() {
        String script = "Mot hai ba bon nam sau bay tam chin muoi muoi mot muoi hai muoi ba muoi bon.";

        List<SubtitleGenerator.Cue> cues = generator.distribute(script, 8.0);

        String joined = String.join(" ",
                cues.stream().map(c -> c.text().replace("\n", " ")).toList())
                .replaceAll("\\s+", " ").strip();
        assertThat(joined).isEqualTo(script);
    }

    @Test
    void formatTimestampHandlesRollover() {
        assertThat(SubtitleGenerator.formatTimestamp(0)).isEqualTo("00:00:00,000");
        assertThat(SubtitleGenerator.formatTimestamp(2.5)).isEqualTo("00:00:02,500");
        assertThat(SubtitleGenerator.formatTimestamp(61.5)).isEqualTo("00:01:01,500");
        assertThat(SubtitleGenerator.formatTimestamp(3661.007)).isEqualTo("01:01:01,007");
    }

    @Test
    void emptyScriptThrows() {
        assertThatThrownBy(() -> generator.generate("   ", 5.0))
                .isInstanceOf(SubtitleException.class)
                .satisfies(ex -> assertThat(((SubtitleException) ex).getCode())
                        .isEqualTo("SUBTITLE_FAILED"));
    }

    @Test
    void nonPositiveDurationThrows() {
        assertThatThrownBy(() -> generator.generate("hello", 0))
                .isInstanceOf(SubtitleException.class);
    }
}
