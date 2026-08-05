package com.stop.identity_service.userProfile.service.avatar;

import com.stop.identity_service.common.error.IdentityErrorCode;
import com.stop.identity_service.common.exception.BusinessException;
import com.stop.identity_service.config.aws.AwsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;

/**
 * Validates, re-encodes, and stores profile photos in S3. Re-encoding (decode -> fixed max
 * dimension -> re-save as JPEG) is the primary defense against disguised/polyglot uploads: the
 * output is always genuine, normalized image data regardless of what was submitted.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AvatarStorageService {

    private static final int MAX_DIMENSION = 1024;
    private static final String KEY_PREFIX = "avatars/";

    private final S3Client s3Client;
    private final AwsProperties awsProperties;

    public String uploadAvatar(byte[] originalBytes) {
        BufferedImage decoded = decode(originalBytes);
        BufferedImage normalized = normalize(decoded);
        byte[] jpegBytes = encodeAsJpeg(normalized);

        String key = KEY_PREFIX + UUID.randomUUID() + ".jpg";
        // Bucket must allow object ACLs (Object Ownership != "Bucket owner enforced") for this
        // public-read grant to take effect - see contracts/avatar-storage.contract.md.
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(awsProperties.s3().bucket())
                        .key(key)
                        .contentType("image/jpeg")
                        .acl(ObjectCannedACL.PUBLIC_READ)
                        .build(),
                RequestBody.fromBytes(jpegBytes)
        );

        String url = String.format(
                "https://%s.s3.%s.amazonaws.com/%s",
                awsProperties.s3().bucket(), awsProperties.region(), key
        );
        log.info("Avatar uploaded key={}", key);
        return url;
    }

    /** Deletes the S3 object referenced by a previously-returned avatar URL. Propagates S3 failures - callers decide whether that should block their operation. */
    public void deleteObject(String avatarUrl) {
        String key = extractKey(avatarUrl);
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(awsProperties.s3().bucket())
                .key(key)
                .build());
        log.info("Avatar deleted key={}", key);
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

    private byte[] encodeAsJpeg(BufferedImage image) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (!ImageIO.write(image, "jpg", out)) {
                throw new BusinessException(IdentityErrorCode.INVALID_IMAGE_FILE);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(IdentityErrorCode.INVALID_IMAGE_FILE);
        }
    }

    private String extractKey(String avatarUrl) {
        String path = URI.create(avatarUrl).getPath();
        return path.startsWith("/") ? path.substring(1) : path;
    }
}
