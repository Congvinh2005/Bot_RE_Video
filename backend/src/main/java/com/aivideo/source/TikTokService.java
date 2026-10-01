package com.aivideo.source;

import com.aivideo.common.exception.ResourceNotFoundException;
import com.aivideo.project.Project;
import com.aivideo.project.ProjectRepository;
import com.aivideo.source.dto.TikTokRequest;
import com.aivideo.source.dto.TikTokResponse;
import com.aivideo.source.dto.VideoSourceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TikTokService {

    private final ProjectRepository projectRepository;
    private final VideoSourceRepository videoSourceRepository;
    private final List<SourceProvider> sourceProviders;

    @Transactional
    public TikTokResponse analyzeTikTok(UUID projectId, TikTokRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND",
                        "Project not found: " + projectId));
        String url = request.url() != null ? request.url().trim() : "";
        if (!TikTokUrlValidator.isTikTokUrl(url)) {
            throw new VideoUploadException("INVALID_URL",
                    "Not a valid TikTok URL", HttpStatus.BAD_REQUEST);
        }
        SourceProvider provider = sourceProviders.stream()
                .filter(p -> p.supports(url))
                .findFirst()
                .orElseThrow(() -> new VideoUploadException("UNSUPPORTED_SOURCE",
                        "No provider supports this URL", HttpStatus.BAD_REQUEST));

        SourceAnalysis analysis = provider.analyze(url, request.context());
        VideoSource source = videoSourceRepository.save(VideoSource.builder()
                .project(project)
                .sourceUrl(url)
                .sourceType(SourceType.TIKTOK)
                .status(analysis.status())
                .build());
        log.info("TIKTOK_ANALYZED project={} status={} hasMetadata={}",
                projectId, analysis.status(), analysis.metadata() != null);
        return new TikTokResponse(analysis.status(), analysis.message(),
                analysis.metadata(), VideoSourceResponse.from(source));
    }
}
