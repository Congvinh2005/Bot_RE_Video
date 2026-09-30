package com.aivideo.media;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {
    List<MediaAsset> findByProjectId(UUID projectId);
    List<MediaAsset> findByProjectIdAndAssetType(UUID projectId, AssetType assetType);
    Optional<MediaAsset> findByStorageKey(String storageKey);
}
