package com.stop.identity_service.service;

import com.stop.identity_service.common.error.IdentityErrorCode;
import com.stop.identity_service.common.exception.BusinessException;
import com.stop.identity_service.config.aws.AwsProperties;
import com.stop.identity_service.userProfile.service.avatar.AvatarStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvatarStorageServiceTest {

    @Mock
    private S3Client s3Client;

    private final AwsProperties awsProperties =
            new AwsProperties("eu-central-1", new AwsProperties.S3("test-bucket"));

    private AvatarStorageService service;

    private byte[] pngBytes(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    @Test
    void uploadAvatar_validImage_uploadsAndReturnsUrl() throws IOException {
        service = new AvatarStorageService(s3Client, awsProperties);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String url = service.uploadAvatar(pngBytes(200, 100));

        assertTrue(url.startsWith("https://test-bucket.s3.eu-central-1.amazonaws.com/avatars/"));
        assertTrue(url.endsWith(".jpg"));

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        assertEquals("test-bucket", requestCaptor.getValue().bucket());
        assertEquals("image/jpeg", requestCaptor.getValue().contentType());
    }

    @Test
    void uploadAvatar_corruptBytes_throwsInvalidImageFile() {
        service = new AvatarStorageService(s3Client, awsProperties);
        byte[] garbage = new byte[]{1, 2, 3, 4, 5};

        BusinessException ex = assertThrows(BusinessException.class, () -> service.uploadAvatar(garbage));

        assertEquals(IdentityErrorCode.INVALID_IMAGE_FILE, ex.getErrorCode());
    }

    @Test
    void uploadAvatar_oversizedImage_isDownscaledBeforeUpload() throws IOException {
        service = new AvatarStorageService(s3Client, awsProperties);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);
        service.uploadAvatar(pngBytes(2000, 1000));

        verify(s3Client).putObject(any(PutObjectRequest.class), bodyCaptor.capture());
        BufferedImage reencoded = ImageIO.read(bodyCaptor.getValue().contentStreamProvider().newStream());
        assertEquals(1024, reencoded.getWidth());
        assertEquals(512, reencoded.getHeight());
    }

    @Test
    void deleteObject_issuesDeleteWithKeyFromUrl() {
        service = new AvatarStorageService(s3Client, awsProperties);
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        service.deleteObject("https://test-bucket.s3.eu-central-1.amazonaws.com/avatars/abc-123.jpg");

        ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(requestCaptor.capture());
        assertEquals("test-bucket", requestCaptor.getValue().bucket());
        assertEquals("avatars/abc-123.jpg", requestCaptor.getValue().key());
    }
}
