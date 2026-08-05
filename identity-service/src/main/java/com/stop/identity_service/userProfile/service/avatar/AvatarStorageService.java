package com.stop.identity_service.userProfile.service.avatar;

import com.stop.identity_service.common.error.IdentityErrorCode;
import com.stop.identity_service.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Validates, re-encodes, and stores profile photos on local disk (a mounted volume). Re-encoding
 * (decode -> fixed max dimension -> re-save as JPEG) is the primary defense against
 * disguised/polyglot uploads: the output is always genuine, normalized image data regardless of
 * what was submitted. Filenames are always a freshly generated UUID - never derived from user
 * input - so path traversal isn't reachable through this class or the read-back endpoint.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(AvatarStorageProperties.class)
public class AvatarStorageService {

    private static final int MAX_DIMENSION = 1024;

    private final AvatarStorageProperties properties;

    public String uploadAvatar(byte[] originalBytes) {
        BufferedImage decoded = decode(originalBytes);
        BufferedImage normalized = normalize(decoded);

        String filename = UUID.randomUUID() + ".jpg";
        Path directory = Path.of(properties.directory());
        Path target = directory.resolve(filename);

        try {
            Files.createDirectories(directory);
            Path tempFile = Files.createTempFile(directory, "upload-", ".tmp");
            try {
                if (!ImageIO.write(normalized, "jpg", tempFile.toFile())) {
                    throw new BusinessException(IdentityErrorCode.INVALID_IMAGE_FILE);
                }
                // Atomic rename within the same directory/filesystem - a reader can never
                // observe a partially-written file under the final name.
                Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } finally {
                Files.deleteIfExists(tempFile);
            }
        } catch (IOException e) {
            throw new BusinessException(IdentityErrorCode.INVALID_IMAGE_FILE);
        }

        log.info("Avatar saved file={}", filename);
        return properties.publicBaseUrl() + "/users/profile/avatar/" + filename;
    }

    /** Deletes the file referenced by a previously-returned avatar URL. Propagates IO failures - callers decide whether that should block their operation. */
    public void deleteObject(String avatarUrl) {
        String filename = validateAndNormalizeFilename(extractFilename(avatarUrl));
        Path target = Path.of(properties.directory()).resolve(filename);
        try {
            Files.deleteIfExists(target);
            log.info("Avatar deleted file={}", filename);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete avatar file " + filename, e);
        }
    }

    /** Loads a previously-uploaded avatar for serving. Filename MUST be validated as our own UUID format before it ever touches the filesystem. */
    public Resource loadAvatar(String filename) {
        String safeFilename = validateAndNormalizeFilename(filename);
        Path target = Path.of(properties.directory()).resolve(safeFilename);
        Resource resource = new FileSystemResource(target);
        if (!resource.exists() || !resource.isReadable()) {
            throw new BusinessException(IdentityErrorCode.AVATAR_NOT_FOUND);
        }
        return resource;
    }

    private BufferedImage decode(byte[] bytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                throw new BusinessException(IdentityErrorCode.INVALID_IMAGE_FILE);
            }
            return image;
        } catch (IOException e) {
            throw new BusinessException(IdentityErrorCode.INVALID_IMAGE_FILE);
        }
    }

    private BufferedImage normalize(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        double scale = Math.min(1.0, (double) MAX_DIMENSION / Math.max(width, height));
        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));

        BufferedImage normalized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = normalized.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            // Fixed white background: source may have an alpha channel (PNG), and JPEG has none.
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, targetWidth, targetHeight);
            g.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g.dispose();
        }
        return normalized;
    }

    private String extractFilename(String avatarUrl) {
        String path = URI.create(avatarUrl).getPath();
        int idx = path.lastIndexOf('/');
        return idx >= 0 ? path.substring(idx + 1) : path;
    }

    /**
     * Rejects anything that isn't exactly "<valid-uuid>.jpg" - the only shape this class ever
     * generates. This is what makes {@link #loadAvatar} and {@link #deleteObject} safe to build a
     * filesystem path from a value that ultimately came from an HTTP path segment: a traversal
     * payload like "../../etc/passwd" can never pass UUID.fromString and never reaches Path.resolve.
     */
    private String validateAndNormalizeFilename(String filename) {
        if (filename == null || !filename.endsWith(".jpg")) {
            throw new BusinessException(IdentityErrorCode.AVATAR_NOT_FOUND);
        }
        String uuidPart = filename.substring(0, filename.length() - 4);
        try {
            UUID.fromString(uuidPart);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(IdentityErrorCode.AVATAR_NOT_FOUND);
        }
        return uuidPart + ".jpg";
    }
}
