package com.stop.identity_service.service;

import com.stop.identity_service.common.error.IdentityErrorCode;
import com.stop.identity_service.common.exception.BusinessException;
import com.stop.identity_service.userProfile.service.avatar.AvatarStorageProperties;
import com.stop.identity_service.userProfile.service.avatar.AvatarStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvatarStorageServiceTest {

    @TempDir
    Path tempDir;

    private AvatarStorageService service;

    @BeforeEach
    void setUp() {
        AvatarStorageProperties properties = new AvatarStorageProperties(
                tempDir.toString(), "http://localhost:8080/api/v1"
        );
        service = new AvatarStorageService(properties);
    }

    private byte[] pngBytes(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    @Test
    void uploadAvatar_validImage_savesFileAndReturnsUrl() throws IOException {
        String url = service.uploadAvatar(pngBytes(200, 100));

        assertTrue(url.startsWith("http://localhost:8080/api/v1/users/profile/avatar/"));
        assertTrue(url.endsWith(".jpg"));

        String filename = url.substring(url.lastIndexOf('/') + 1);
        Path savedFile = tempDir.resolve(filename);
        assertTrue(Files.exists(savedFile));
        // Saved bytes must be genuine, re-encoded JPEG - not the original PNG bytes.
        BufferedImage reencoded = ImageIO.read(savedFile.toFile());
        assertEquals(200, reencoded.getWidth());
        assertEquals(100, reencoded.getHeight());
    }

    @Test
    void uploadAvatar_corruptBytes_throwsInvalidImageFile() {
        byte[] garbage = new byte[]{1, 2, 3, 4, 5};

        BusinessException ex = assertThrows(BusinessException.class, () -> service.uploadAvatar(garbage));

        assertEquals(IdentityErrorCode.INVALID_IMAGE_FILE, ex.getErrorCode());
    }

    @Test
    void uploadAvatar_oversizedImage_isDownscaledBeforeSaving() throws IOException {
        String url = service.uploadAvatar(pngBytes(2000, 1000));

        String filename = url.substring(url.lastIndexOf('/') + 1);
        BufferedImage reencoded = ImageIO.read(tempDir.resolve(filename).toFile());
        assertEquals(1024, reencoded.getWidth());
        assertEquals(512, reencoded.getHeight());
    }

    @Test
    void deleteObject_removesTheFileReferencedByUrl() throws IOException {
        String url = service.uploadAvatar(pngBytes(200, 100));
        String filename = url.substring(url.lastIndexOf('/') + 1);
        assertTrue(Files.exists(tempDir.resolve(filename)));

        service.deleteObject(url);

        assertFalse(Files.exists(tempDir.resolve(filename)));
    }

    @Test
    void loadAvatar_existingFile_returnsReadableResource() throws IOException {
        String url = service.uploadAvatar(pngBytes(200, 100));
        String filename = url.substring(url.lastIndexOf('/') + 1);

        var resource = service.loadAvatar(filename);

        assertTrue(resource.exists());
        assertTrue(resource.isReadable());
    }

    @Test
    void loadAvatar_pathTraversalAttempt_isRejectedNotResolved() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.loadAvatar("../../../../etc/passwd"));

        assertEquals(IdentityErrorCode.AVATAR_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void loadAvatar_nonExistentButWellFormedFilename_throwsNotFound() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.loadAvatar("00000000-0000-0000-0000-000000000000.jpg"));

        assertEquals(IdentityErrorCode.AVATAR_NOT_FOUND, ex.getErrorCode());
    }
}
