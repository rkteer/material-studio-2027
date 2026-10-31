package com.materialstudio.controller;

import com.materialstudio.dto.AssetView;
import com.materialstudio.security.SessionAuthFilter;
import com.materialstudio.service.AssetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/assets")
public class AssetController {
    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @PostMapping
    ResponseEntity<AssetView> upload(
            @RequestAttribute(SessionAuthFilter.USER_ID_ATTRIBUTE) String userId,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.status(201).body(assetService.upload(userId, file));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(
            @RequestAttribute(SessionAuthFilter.USER_ID_ATTRIBUTE) String userId,
            @PathVariable String id
    ) {
        assetService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
