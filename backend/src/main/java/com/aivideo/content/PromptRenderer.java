package com.aivideo.content;

import com.aivideo.ai.AiException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Nạp prompt theo type + version từ resources/prompts/{TYPE}_v{version}.md
 * và render placeholder {{key}}. Đổi version không cần sửa code.
 */
@Component
public class PromptRenderer {

    public PromptTemplate load(PromptType type, int version) {
        String path = "prompts/" + type.name() + "_v" + version + ".md";
        try {
            ClassPathResource resource = new ClassPathResource(path);
            if (!resource.exists()) {
                throw new AiException("PROMPT_NOT_FOUND",
                        "Prompt not found: " + path);
            }
            String content = resource.getContentAsString(StandardCharsets.UTF_8);
            return new PromptTemplate(type, version, content);
        } catch (AiException e) {
            throw e;
        } catch (Exception e) {
            throw new AiException("PROMPT_NOT_FOUND", "Unable to load prompt: " + path, e);
        }
    }

    public String render(PromptType type, int version, Map<String, String> variables) {
        String content = load(type, version).content();
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            content = content.replace("{{" + entry.getKey() + "}}",
                    entry.getValue() != null ? entry.getValue() : "");
        }
        return content;
    }
}
