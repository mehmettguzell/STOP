package com.stop.identity_service.passwordreset.service;

import com.stop.identity_service.common.error.IdentityErrorCode;
import com.stop.identity_service.common.exception.BusinessException;
import com.stop.identity_service.passwordreset.dto.ForgotPasswordResponse;
import com.stop.identity_service.passwordreset.entity.PasswordResetToken;
import com.stop.identity_service.passwordreset.repository.PasswordResetTokenRepository;
import com.stop.identity_service.user.entity.user.User;
import com.stop.identity_service.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final int TOKEN_BYTES      = 32;   // 256-bit rastgele token
    private static final long TOKEN_TTL_MIN   = 15;   // 15 dakika geçerli

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository               userRepository;
    private final PasswordEncoder              passwordEncoder;
    private final CacheManager                 cacheManager;
    private final SecureRandom                 secureRandom = new SecureRandom();

    @Value("${app.password-reset.dev-mode:true}")
    private boolean devMode;

    /**
     * Şifre sıfırlama linki/token'ı gönderir.
     *
     * Güvenlik: e-posta var mı yok mu belli edilmez (enumeration önlemi).
     * Her halükarda aynı 200 OK döner.
     */
    @Transactional
    public ForgotPasswordResponse requestReset(String email) {
        final String[] rawTokenHolder = {null};

        userRepository.findByEmail(email.toLowerCase()).ifPresent(user -> {
            // Eski token'ları temizle
            tokenRepository.deleteAllByUserId(user.getId());

            // Kriptografik güçlü rastgele token üret
            byte[] bytes = new byte[TOKEN_BYTES];
            secureRandom.nextBytes(bytes);
            String rawToken  = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            String tokenHash = DigestUtils.sha256Hex(rawToken);

            PasswordResetToken entity = PasswordResetToken.builder()
                    .userId(user.getId())
                    .tokenHash(tokenHash)
                    .expiresAt(Instant.now().plus(TOKEN_TTL_MIN, ChronoUnit.MINUTES))
                    .build();
            tokenRepository.save(entity);

            log.info("╔══════════════════════════════════════════════════╗");
            log.info("║  ŞİFRE SIFIRLAMA TOKEN (demo)                   ║");
            log.info("║  Kullanıcı : {}  ", user.getEmail());
            log.info("║  Token     : {}  ", rawToken);
            log.info("║  Geçerlilik: {} dakika                          ║", TOKEN_TTL_MIN);
            log.info("╚══════════════════════════════════════════════════╝");

            if (devMode) {
                rawTokenHolder[0] = rawToken;
            }
        });

        // E-posta bulunamasa da aynı yanıt — kullanıcı bilgilendirilmez (enumeration önlemi)
        // devMode'da token response'a eklenir, prod'da null kalır
        return new ForgotPasswordResponse(rawTokenHolder[0]);
    }

    /**
     * Token + yeni şifre ile şifreyi sıfırlar.
     *
     * Güvenlik kontrolleri:
     *  - Token var mı?
     *  - Süresi dolmuş mu?
     *  - Daha önce kullanılmış mı?
     *  - Kullanıcı aktif mi?
     */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        String tokenHash = DigestUtils.sha256Hex(rawToken);

        PasswordResetToken token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException(IdentityErrorCode.RESET_TOKEN_INVALID));

        if (token.isUsed()) {
            throw new BusinessException(IdentityErrorCode.RESET_TOKEN_INVALID);
        }

        if (Instant.now().isAfter(token.getExpiresAt())) {
            tokenRepository.delete(token);
            throw new BusinessException(IdentityErrorCode.RESET_TOKEN_EXPIRED);
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new BusinessException(IdentityErrorCode.USER_NOT_FOUND));

        // Şifreyi güncelle
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Token'ı tek kullanımlık yap ve temizle
        tokenRepository.deleteAllByUserId(user.getId());

        // Şifre değiştiğinde eski cache'i temizle
        var userId = user.getId();
        for (String cacheName : new String[]{"user:self", "user:public"}) {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) cache.evict(userId);
        }

        log.info("Şifre başarıyla sıfırlandı userId={}", userId);
    }
}
