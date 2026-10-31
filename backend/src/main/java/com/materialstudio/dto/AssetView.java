package com.materialstudio.dto;

import com.materialstudio.domain.Asset;

public record AssetView(
        String id,
        String originalName,
        String mimeType,
        long sizeBytes,
        int width,
        int height
) {
    public static AssetView from(Asset asset) {
        return new AssetView(
                asset.getId(),
                asset.getOriginalName(),
                asset.getMimeType(),
                asset.getSizeBytes(),
                asset.getWidth(),
                asset.getHeight()
        );
    }
}
