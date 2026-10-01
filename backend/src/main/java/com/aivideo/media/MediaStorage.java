package com.aivideo.media;

import java.io.InputStream;

/**
 * Abstraction lưu trữ media. Mọi binary (video/audio/subtitle/thumbnail)
 * nằm ở object storage, PostgreSQL chỉ giữ metadata + storage key.
 */
public interface MediaStorage {

    /** Upload và trả về object key đã lưu. */
    String upload(String key, InputStream data, long size, String contentType);

    /** Download stream của object. Caller tự đóng stream. */
    InputStream download(String key);

    void delete(String key);

    boolean exists(String key);

    /** URL có thời hạn để preview/download. */
    String getUrl(String key);
}
