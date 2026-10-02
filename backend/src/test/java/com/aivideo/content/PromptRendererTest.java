package com.aivideo.content;

import com.aivideo.ai.AiException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromptRendererTest {

    private final PromptRenderer renderer = new PromptRenderer();

    @Test
    void loadFindsVersionedPrompt() {
        PromptTemplate template = renderer.load(PromptType.CONTENT_GENERATION, 1);

        assertThat(template.type()).isEqualTo(PromptType.CONTENT_GENERATION);
        assertThat(template.version()).isEqualTo(1);
        assertThat(template.content()).contains("{{unified_context}}");
    }

    @Test
    void renderReplacesVariables() {
        String rendered = renderer.render(PromptType.CAPTION_GENERATION, 1,
                Map.of("product_context", "Bo ngu", "language", "vi"));

        assertThat(rendered).contains("Bo ngu");
        assertThat(rendered).contains("vi");
        assertThat(rendered).doesNotContain("{{product_context}}");
    }

    @Test
    void renderNullVariableBecomesEmpty() {
        java.util.HashMap<String, String> vars = new java.util.HashMap<>();
        vars.put("script", null);

        String rendered = renderer.render(PromptType.VOICE_SCRIPT, 1, vars);

        assertThat(rendered).doesNotContain("{{script}}");
    }

    @Test
    void unknownVersionThrows() {
        assertThatThrownBy(() -> renderer.load(PromptType.VIDEO_ANALYSIS, 99))
                .isInstanceOf(AiException.class)
                .satisfies(ex -> assertThat(((AiException) ex).getCode())
                        .isEqualTo("PROMPT_NOT_FOUND"));
    }

    @Test
    void allPromptTypesHaveV1() {
        for (PromptType type : PromptType.values()) {
            assertThat(renderer.load(type, 1).content()).isNotBlank();
        }
    }

    @Test
    void v2EnforcesTikTokRules() {
        String content = renderer.load(PromptType.CONTENT_GENERATION, 2).content();

        assertThat(content).contains("3 giây");
        assertThat(content).contains("15-30");
        assertThat(content).contains("3-5");

        String caption = renderer.load(PromptType.CAPTION_GENERATION, 2).content();

        assertThat(caption).contains("3-5");
    }
}
