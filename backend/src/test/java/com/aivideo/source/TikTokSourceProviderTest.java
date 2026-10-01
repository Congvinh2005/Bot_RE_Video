package com.aivideo.source;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TikTokSourceProviderTest {

    @Mock
    TikTokOEmbedClient oEmbedClient;

    @InjectMocks
    TikTokSourceProvider provider;

    private static final String URL = "https://www.tiktok.com/@user/video/123";

    @Test
    void supportsTikTokUrlsOnly() {
        assertThat(provider.supports(URL)).isTrue();
        assertThat(provider.supports("https://youtube.com/watch?v=1")).isFalse();
    }

    @Test
    void analyzeReturnsMetadataWhenOEmbedSucceeds() {
        when(oEmbedClient.fetch(URL)).thenReturn(Optional.of(
                new OEmbedData("Funny video", "someuser",
                        "https://tiktok.com/@someuser", "https://thumb.jpg")));

        SourceAnalysis analysis = provider.analyze(URL, "my context");

        assertThat(analysis.status()).isEqualTo(SourceStatus.USER_UPLOAD_REQUIRED);
        assertThat(analysis.metadata()).isNotNull();
        assertThat(analysis.metadata().title()).isEqualTo("Funny video");
        assertThat(analysis.metadata().author()).isEqualTo("someuser");
        assertThat(analysis.message()).isNotBlank();
    }

    @Test
    void analyzeStillRequiresUploadWhenOEmbedFails() {
        when(oEmbedClient.fetch(URL)).thenReturn(Optional.empty());

        SourceAnalysis analysis = provider.analyze(URL, null);

        assertThat(analysis.status()).isEqualTo(SourceStatus.USER_UPLOAD_REQUIRED);
        assertThat(analysis.metadata()).isNull();
        assertThat(analysis.message()).containsIgnoringCase("upload");
    }
}
