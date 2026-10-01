package com.aivideo.source;

import com.aivideo.common.exception.ResourceNotFoundException;
import com.aivideo.media.AssetType;
import com.aivideo.media.MediaAsset;
import com.aivideo.media.MediaAssetRepository;
import com.aivideo.media.MediaStorage;
import com.aivideo.media.StoragePath;
import com.aivideo.project.Project;
import com.aivideo.project.ProjectRepository;
import com.aivideo.source.dto.VideoSourceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadService {

    private final ProjectRepository projectRepository;
    private final VideoSourceRepository videoSourceRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final MediaStorage mediaStorage;
    private final VideoMetadataService metadataService;
    private final UploadProperties properties;

    @Transactional
    public VideoSourceResponse uploadVideo(UUID projectId, MultipartFile file) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND",
                        "Project not found: " + projectId));
        validate(file);
        String filename = file.getOriginalFilename() != null
                && !file.getOriginalFilename().isBlank()
                ? file.getOriginalFilename() : "unnamed";

        Path temp = null;
        try {
            temp = Files.createTempFile("upload-", "-" + filename.replaceAll("[^a-zA-Z0-9._-]", "_"));
            file.transferTo(temp);
            VideoMetadata meta = metadataService.probe(temp);

            String key = StoragePath.pathFor(AssetType.VIDEO, filename);
            long size = Files.size(temp);
            try (InputStream in = Files.newInputStream(temp)) {
                mediaStorage.upload(key, in, size, file.getContentType());
            }

            VideoSource source = videoSourceRepository.save(VideoSource.builder()
                    .project(project)
                    .sourceType(SourceType.UPLOAD)
                    .originalFilename(filename)
                    .storageKey(key)
                    .duration(meta.duration())
                    .width(meta.width())
                    .height(meta.height())
                    .fps(meta.fps())
                    .codec(meta.videoCodec())
                    .hasAudio(meta.hasAudio())
                    .format(meta.format())
                    .status(SourceStatus.READY)
                    .build());
            mediaAssetRepository.save(MediaAsset.builder()
                    .project(project)
                    .assetType(AssetType.VIDEO)
                    .storageKey(key)
                    .filename(filename)
                    .mimeType(file.getContentType())
                    .sizeBytes(file.getSize())
                    .build());
            log.info("UPLOAD_OK project={} key={} duration={}", projectId, key, meta.duration());
            return VideoSourceResponse.from(source);
        } catch (VideoUploadException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new VideoUploadException("UPLOAD_FAILED", "Upload failed",
                    e, HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new VideoUploadException("UPLOAD_FAILED", "File is empty", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > properties.maxFileSizeBytes()) {
            throw new VideoUploadException("FILE_TOO_LARGE",
                    "File exceeds max size of " + properties.maxFileSizeMb() + "MB",
                    HttpStatus.PAYLOAD_TOO_LARGE);
        }
        String filename = file.getOriginalFilename();
        String ext = filename != null && filename.contains(".")
                ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!properties.allowedExtensions().contains(ext)) {
            throw new VideoUploadException("VIDEO_FORMAT_INVALID",
                    "Unsupported video format: " + ext + ". Allowed: " + properties.allowedExtensions(),
                    HttpStatus.BAD_REQUEST);
        }
        if (file.getContentType() == null
                || !properties.allowedMimeTypes().contains(file.getContentType().toLowerCase())) {
            throw new VideoUploadException("VIDEO_FORMAT_INVALID",
                    "Unsupported MIME type: " + file.getContentType(),
                    HttpStatus.BAD_REQUEST);
        }
    }
}
