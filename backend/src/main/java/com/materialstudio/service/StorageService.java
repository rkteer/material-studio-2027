package com.materialstudio.service;

import com.materialstudio.error.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class StorageService {
    private static final long MAX_UPLOAD_BYTES = 100L * 1024 * 1024;
    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    private final Path assetDirectory;
    private final Path resultDirectory;
    private final long maxPixels;

    public StorageService(
            @Value("${app.storage.root}") String storageRoot,
            @Value("${app.storage.max-pixels}") long maxPixels
    ) {
        this.assetDirectory = Path.of(storageRoot).toAbsolutePath().normalize().resolve("assets");
        this.resultDirectory = Path.of(storageRoot).toAbsolutePath().normalize().resolve("results");
        this.maxPixels = maxPixels;
        try {
            Files.createDirectories(assetDirectory);
            Files.createDirectories(resultDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to initialize media storage", exception);
        }
    }

    public PreparedImage storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_FILE", "请选择要上传的图片");
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "图片不能超过 100MB");
        }

        String imageType;
        String extension;
        try {
            imageType = detectImageType(file);
            extension = "image/png".equals(imageType) ? ".png" : ".jpg";
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "FILE_READ_FAILED", "无法读取上传文件");
        }

        Path temporary = null;
        Path finalPath = null;
        try {
            temporary = Files.createTempFile(assetDirectory, ".upload-", ".tmp");
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            }

            BufferedImage image = ImageIO.read(temporary.toFile());
            if (image == null) {
                throw new ApiException(
                        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "INVALID_IMAGE",
                        "文件内容不是可读取的 JPEG 或 PNG 图片"
                );
            }
            long pixels = (long) image.getWidth() * image.getHeight();
            if (pixels <= 0 || pixels > maxPixels) {
                throw new ApiException(
                        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "IMAGE_TOO_LARGE",
                        "图片像素尺寸过大，请压缩后重试"
                );
            }

            String storageName = UUID.randomUUID() + extension;
            finalPath = safeResolve(assetDirectory, storageName);
            Files.move(temporary, finalPath, StandardCopyOption.REPLACE_EXISTING);
            temporary = null;
            return new PreparedImage(
                    storageName,
                    finalPath,
                    imageType,
                    file.getSize(),
                    image.getWidth(),
                    image.getHeight()
            );
        } catch (ApiException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to store uploaded image", exception);
        } finally {
            deleteQuietly(temporary);
            if (finalPath != null && !Files.exists(finalPath)) {
                finalPath = null;
            }
        }
    }

    public Path resolveAsset(String storageName) {
        Path path = safeResolve(assetDirectory, storageName);
        if (!Files.isRegularFile(path)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ASSET_FILE_MISSING", "素材文件不存在");
        }
        return path;
    }

    public Path createTemporaryResult(String taskId) {
        try {
            return Files.createTempFile(resultDirectory, taskId + "-", ".tmp");
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to allocate result file", exception);
        }
    }

    public String commitResult(String taskId, Path temporary) {
        String storageName = taskId + ".png";
        Path finalPath = safeResolve(resultDirectory, storageName);
        try {
            Files.move(temporary, finalPath, StandardCopyOption.REPLACE_EXISTING);
            return storageName;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to store generated result", exception);
        }
    }

    public Path resolveResult(String storageName) {
        Path path = safeResolve(resultDirectory, storageName);
        if (!Files.isRegularFile(path)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "RESULT_FILE_MISSING", "制作结果文件不存在");
        }
        return path;
    }

    public void deleteAsset(String storageName) {
        deleteQuietly(safeResolve(assetDirectory, storageName));
    }

    public void deleteResult(String storageName) {
        if (storageName != null) {
            deleteQuietly(safeResolve(resultDirectory, storageName));
        }
    }

    public void deleteTemporaryResult(Path path) {
        deleteQuietly(path);
    }

    private String detectImageType(MultipartFile file) throws IOException {
        byte[] header;
        try (InputStream input = file.getInputStream()) {
            header = input.readNBytes(8);
        }
        boolean png = header.length == 8;
        for (int index = 0; png && index < PNG_SIGNATURE.length; index++) {
            png = header[index] == PNG_SIGNATURE[index];
        }
        if (png) {
            return "image/png";
        }
        if (header.length >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        throw new ApiException(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "UNSUPPORTED_IMAGE",
                "仅支持 JPEG 或 PNG 图片"
        );
    }

    private Path safeResolve(Path directory, String filename) {
        if (filename == null || filename.isBlank()) {
            throw new IllegalStateException("Media filename is missing");
        }
        Path resolved = directory.resolve(filename).normalize();
        if (!resolved.startsWith(directory)) {
            throw new IllegalStateException("Invalid media filename");
        }
        return resolved;
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // A periodic storage cleanup can remove an orphaned development file later.
        }
    }

    public record PreparedImage(
            String storageName,
            Path path,
            String mimeType,
            long sizeBytes,
            int width,
            int height
    ) {
    }
}
