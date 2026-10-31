package com.materialstudio.service;

import com.materialstudio.domain.Asset;
import com.materialstudio.dto.AssetView;
import com.materialstudio.error.ApiException;
import com.materialstudio.mapper.AssetMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AssetService {
    private final AssetMapper assetMapper;
    private final StorageService storageService;

    public AssetService(AssetMapper assetMapper, StorageService storageService) {
        this.assetMapper = assetMapper;
        this.storageService = storageService;
    }

    @Transactional
    public AssetView upload(String userId, MultipartFile file) {
        StorageService.PreparedImage prepared = storageService.storeImage(file);
        Asset asset = new Asset();
        asset.setId(UUID.randomUUID().toString());
        asset.setUserId(userId);
        asset.setStorageName(prepared.storageName());
        asset.setOriginalName(cleanName(file.getOriginalFilename()));
        asset.setMimeType(prepared.mimeType());
        asset.setSizeBytes(prepared.sizeBytes());
        asset.setWidth(prepared.width());
        asset.setHeight(prepared.height());
        asset.setCreatedAt(LocalDateTime.now());
        try {
            assetMapper.insert(asset);
        } catch (RuntimeException exception) {
            storageService.deleteAsset(prepared.storageName());
            throw exception;
        }
        return AssetView.from(asset);
    }

    @Transactional
    public void delete(String userId, String assetId) {
        Asset asset = assetMapper.findByIdAndUser(assetId, userId);
        if (asset == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ASSET_NOT_FOUND", "素材不存在");
        }
        if (assetMapper.deleteIfUnused(assetId, userId) == 0) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "ASSET_IN_USE",
                    "素材已用于制作任务，不能移除"
            );
        }
        storageService.deleteAsset(asset.getStorageName());
    }

    private String cleanName(String originalName) {
        String fallback = "image";
        if (originalName == null || originalName.isBlank()) {
            return fallback;
        }
        String cleaned = originalName.replace('\\', '/');
        int slash = cleaned.lastIndexOf('/');
        cleaned = slash >= 0 ? cleaned.substring(slash + 1) : cleaned;
        return cleaned.length() > 240 ? cleaned.substring(0, 240) : cleaned;
    }
}
