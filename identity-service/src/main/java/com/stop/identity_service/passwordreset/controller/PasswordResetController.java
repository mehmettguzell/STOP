package com.stop.identity_service.passwordreset.controller;

import com.stop.identity_service.passwordreset.dto.ForgotPasswordRequest;
import com.stop.identity_service.passwordreset.dto.ForgotPasswordResponse;
import com.stop.identity_service.passwordreset.dto.ResetPasswordRequest;
import com.stop.identity_service.passwordreset.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    /**
     * E-posta gönderir (veya demo'da loglar).
     * Her zaman 200 döner — e-posta var mı yok mu belli etmez.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        ForgotPasswordResponse response = passwordResetService.requestReset(request.email());
        return ResponseEntity.ok(response);
    }

    /**
     * Token + yeni şifre ile şifreyi sıfırlar.
     * JWT gerektirmez — kullanıcı zaten giriş yapamıyor.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok().build();
    }
}
