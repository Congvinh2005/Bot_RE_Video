package com.aivideo.media;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test với MinIO thật.
 * Yêu cầu MinIO chạy ở {@code app.storage.endpoint} (mặc định localhost:9000):
 * <pre>
 *   minio server /tmp/minio-data --address ":9000"
 *   # hoặc: docker run -p 9000:9000 -e MINIO_ROOT_USER=minioadmin \
 *   #          -e MINIO_ROOT_PASSWORD=minioadmin minio/minio server /data
 * </pre>
 */
@SpringBootTest(classes = {StorageConfig.class, MinioMediaStorage.class})
@TestPropertySource(properties = {
        "app.storage.bucket=test-bucket-it",
        "app.storage.url-expiry-hours=1"
})
class MinioMediaStorageIT {

    @Autowired
    MediaStorage storage;

    @Test
    void uploadDownloadDeleteRoundTripForAllAssetTypes() throws Exception {
        for (AssetType type : AssetType.values()) {
            byte[] content = ("hello-" + type.name()).getBytes(StandardCharsets.UTF_8);
            String key = StoragePath.pathFor(type, "sample.mp4");

            String saved = storage.upload(key,
                    new ByteArrayInputStream(content), content.length, "video/mp4");
            assertThat(saved).isEqualTo(key);
            assertThat(storage.exists(key)).isTrue();

            try (InputStream in = storage.download(key)) {
                assertThat(in.readAllBytes()).isEqualTo(content);
            }

            String url = storage.getUrl(key);
            assertThat(url).startsWith("http");

            storage.delete(key);
            assertThat(storage.exists(key)).isFalse();
        }
    }

    @Test
    void downloadMissingKeyThrowsStorageError() {
        assertThatThrownBy(() -> storage.download("video/not-exists.mp4"))
                .isInstanceOf(StorageException.class)
                .satisfies(ex -> assertThat(((StorageException) ex).getCode())
                        .isEqualTo("STORAGE_ERROR"));
    }
}
