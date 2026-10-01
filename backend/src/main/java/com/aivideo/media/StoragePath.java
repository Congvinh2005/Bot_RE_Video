package com.aivideo.media;

import java.util.UUID;

/** Quy ước prefix key theo loại asset: video/ audio/ subtitle/ thumbnail/ */
public final class StoragePath {

    private StoragePath() {
    }

    public static String pathFor(AssetType assetType, String filename) {
        String prefix = switch (assetType) {
            case VIDEO -> "video";
            case AUDIO -> "audio";
            case SUBTITLE -> "subtitle";
            case THUMBNAIL -> "thumbnail";
        };
        String safeName = filename == null || filename.isBlank()
                ? "file"
                : filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return prefix + "/" + UUID.randomUUID() + "-" + safeName;
    }
}
