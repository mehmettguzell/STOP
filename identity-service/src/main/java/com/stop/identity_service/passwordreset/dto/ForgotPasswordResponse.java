package com.stop.identity_service.passwordreset.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * devToken: sadece dev modunda dolu gelir, prod'da null.
 * Gerçek uygulamada bu alan hiç olmaz — token yalnızca e-posta ile iletilir.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ForgotPasswordResponse(
        String devToken
) {}
