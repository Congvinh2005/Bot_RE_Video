package com.aivideo.content;

/** Một phiên bản prompt. Không hard-code prompt trong Controller/Service. */
public record PromptTemplate(
        PromptType type,
        int version,
        String content
) {
}
