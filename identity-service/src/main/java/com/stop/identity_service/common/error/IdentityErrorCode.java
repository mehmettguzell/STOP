package com.stop.identity_service.common.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor

public enum IdentityErrorCode implements ErrorCode {

    USER_NOT_FOUND("USER_NOT_FOUND", "Kullanici bulunamadi", HttpStatus.NOT_FOUND),

    EMAIL_ALREADY_EXISTS("EMAIL_ALREADY_EXISTS", "Bu e-posta adresi zaten kullanimda", HttpStatus.CONFLICT),

    NAME_ALREADY_EXISTS("NAME_ALREADY_EXISTS", "Bu kullanici adi zaten kullanimda", HttpStatus.CONFLICT),

    PHONE_ALREADY_EXISTS("PHONE_ALREADY_EXISTS", "Bu telefon numarasi zaten kullanimda", HttpStatus.CONFLICT),

    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "E-posta veya sifre hatali", HttpStatus.UNAUTHORIZED),

    USER_SUSPENDED("USER_SUSPENDED", "Hesabiniz askiya alinmistir", HttpStatus.FORBIDDEN),

    USER_DELETED("USER_DELETED", "Bu hesap silinmistir", HttpStatus.GONE),

    PROFILE_ALREADY_EXISTS("PROFILE_ALREADY_EXISTS", "Profil zaten olusturulmus", HttpStatus.CONFLICT),

    PROFILE_NOT_FOUND("PROFILE_NOT_FOUND", "Kullanici profili bulunamadi", HttpStatus.NOT_FOUND),

    USER_NOT_ACTIVE("USER_NOT_ACTIVE", "Kullanici aktif degil", HttpStatus.CONFLICT),

    CANNOT_REPORT_YOURSELF("CANNOT_REPORT_YOURSELF", "Kendi kendinizi raporlayamazsiniz", HttpStatus.BAD_REQUEST),

    REPORT_ALREADY_SENT("REPORT_ALREADY_SENT", "Bu kullanici hakkinda zaten acik veya incelenen bir raporunuz bulunmaktadir", HttpStatus.CONFLICT),

    REPORT_NOT_FOUND("REPORT_NOT_FOUND", "Aranan rapor sistemde bulunamadi", HttpStatus.NOT_FOUND),

    INVALID_STATUS_TRANSITION("INVALID_STATUS_TRANSITION", "Raporun su anki durumu bu guncellemeye uygun degildir", HttpStatus.BAD_REQUEST),

    REPORT_ALREADY_CLOSED("REPORT_ALREADY_CLOSED", "Bu rapor zaten sonuclandirilmis, üzerinde degisiklik yapilamaz", HttpStatus.BAD_REQUEST),

    PROFILE_UPDATE_FORBIDDEN(
            "PROFILE_UPDATE_FORBIDDEN",
            "Baska bir kullanicinin profilini guncelleyemezsiniz",
            HttpStatus.FORBIDDEN
    ),

    PROFILE_VIEW_FORBIDDEN(
            "PROFILE_VIEW_FORBIDDEN",
            "Baska bir kullanicinin profilini goremezsiniz",
            HttpStatus.FORBIDDEN
    ),

    REFRESH_TOKEN_REVOKED(
            "REFRESH_TOKEN_REVOKED",
            "Oturum iptal edilmis, lutfen tekrar giris yapin",
            HttpStatus.UNAUTHORIZED
    ),

    REFRESH_TOKEN_EXPIRED(
            "REFRESH_TOKEN_EXPIRED",
            "Oturum suresi doldu, lutfen tekrar giris yapin",
            HttpStatus.UNAUTHORIZED
    ),

    INVALID_REFRESH_TOKEN(
            "INVALID_REFRESH_TOKEN",
            "Gecersiz oturum, lutfen tekrar giris yapin",
            HttpStatus.UNAUTHORIZED
    ),

    FRIENDSHIP_ALREADY_EXISTS(
            "FRIENDSHIP_ALREADY_EXISTS",
            "Bu kullaniciya zaten istek gonderdiniz veya arkadassiniz",
            HttpStatus.CONFLICT
    ),

    FRIENDSHIP_NOT_FOUND(
            "FRIENDSHIP_NOT_FOUND",
            "Arkadaslik istegi bulunamadi",
            HttpStatus.NOT_FOUND
    ),

    FRIENDSHIP_NOT_PENDING(
            "FRIENDSHIP_NOT_PENDING",
            "Bu istek artik beklemede degil",
            HttpStatus.CONFLICT
    ),

    FRIENDSHIP_FORBIDDEN(
            "FRIENDSHIP_FORBIDDEN",
            "Bu islemi yapmaya yetkiniz yok",
            HttpStatus.FORBIDDEN
    ),

    CANNOT_FRIEND_YOURSELF(
            "CANNOT_FRIEND_YOURSELF",
            "Kendinize arkadaslik istegi gonderemezsiniz",
            HttpStatus.BAD_REQUEST
    ),

    // ── Şifre Sıfırlama ────────────────────────────────────────────────────────
    RESET_TOKEN_INVALID(
            "RESET_TOKEN_INVALID",
            "Geçersiz veya kullanılmış sıfırlama bağlantısı.",
            HttpStatus.BAD_REQUEST
    ),
    RESET_TOKEN_EXPIRED(
            "RESET_TOKEN_EXPIRED",
            "Şifre sıfırlama süresi doldu. Lütfen yeniden talep edin.",
            HttpStatus.GONE
    );




    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}