package com.aivideo.media;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StoragePathTest {

    @Test
    void pathForPrefixesByAssetType() {
        assertThat(StoragePath.pathFor(AssetType.VIDEO, "a.mp4")).startsWith("video/");
        assertThat(StoragePath.pathFor(AssetType.AUDIO, "a.mp3")).startsWith("audio/");
        assertThat(StoragePath.pathFor(AssetType.SUBTITLE, "a.srt")).startsWith("subtitle/");
        assertThat(StoragePath.pathFor(AssetType.THUMBNAIL, "a.jpg")).startsWith("thumbnail/");
    }

    @Test
    void pathForSanitizesFilename() {
        String key = StoragePath.pathFor(AssetType.VIDEO, "bộ ngủ nữ 2 dây.mp4");
        assertThat(key).startsWith("video/");
        assertThat(key).doesNotContain(" ");
        assertThat(key).endsWith(".mp4");
    }

    @Test
    void pathForGeneratesUniqueKeys() {
        String first = StoragePath.pathFor(AssetType.VIDEO, "a.mp4");
        String second = StoragePath.pathFor(AssetType.VIDEO, "a.mp4");
        assertThat(first).isNotEqualTo(second);
    }
}
