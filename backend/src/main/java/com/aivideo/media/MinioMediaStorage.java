package com.aivideo.media;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class MinioMediaStorage implements MediaStorage {

    private final MinioClient minioClient;
    private final StorageProperties properties;

    @Override
    public String upload(String key, InputStream data, long size, String contentType) {
        try {
            ensureBucket();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(key)
                    .stream(data, size, -1)
                    .contentType(contentType)
                    .build());
            log.info("STORAGE_UPLOAD key={} size={}", key, size);
            return key;
        } catch (Exception e) {
            throw new StorageException("STORAGE_ERROR", "Upload failed for key: " + key, e);
        }
    }

    @Override
    public InputStream download(String key) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(key)
                    .build());
        } catch (Exception e) {
            throw new StorageException("STORAGE_ERROR", "Download failed for key: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(key)
                    .build());
            log.info("STORAGE_DELETE key={}", key);
        } catch (Exception e) {
            throw new StorageException("STORAGE_ERROR", "Delete failed for key: " + key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(key)
                    .build());
            return true;
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                return false;
            }
            throw new StorageException("STORAGE_ERROR", "Exists check failed for key: " + key, e);
        } catch (Exception e) {
            throw new StorageException("STORAGE_ERROR", "Exists check failed for key: " + key, e);
        }
    }

    @Override
    public String getUrl(String key) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(properties.bucket())
                    .object(key)
                    .expiry(properties.urlExpiryHours(), TimeUnit.HOURS)
                    .build());
        } catch (Exception e) {
            throw new StorageException("STORAGE_ERROR", "Get URL failed for key: " + key, e);
        }
    }

    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(properties.bucket()).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(properties.bucket()).build());
            log.info("STORAGE_BUCKET_CREATED bucket={}", properties.bucket());
        }
    }
}
