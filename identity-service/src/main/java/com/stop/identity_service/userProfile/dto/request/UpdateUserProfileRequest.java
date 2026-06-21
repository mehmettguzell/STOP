package com.stop.identity_service.userProfile.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record UpdateUserProfileRequest(

        @Size(max = 50, message = "Ad en fazla 50 karakter olabilir")
        @Pattern(regexp = "^[\\p{L}\\p{M}\\s'.-]*$", message = "Ad gecersiz karakterler iceriyor")
        String firstName,

        @Size(max = 50, message = "Soyad en fazla 50 karakter olabilir")
        @Pattern(regexp = "^[\\p{L}\\p{M}\\s'.-]*$", message = "Soyad gecersiz karakterler iceriyor")
        String lastName,

        @Past(message = "Dogum tarihi gecmiste olmalidir")
        LocalDate birthDate,

        @Size(max = 100, message = "Sehir en fazla 100 karakter olabilir")
        String city,

        @Size(max = 100, message = "Pozisyon en fazla 100 karakter olabilir")
        String position,

        @Min(value = 50, message = "Boy en az 50 cm olmalidir")
        @Max(value = 280, message = "Boy en fazla 280 cm olabilir")
        Integer heightCm,

        @Min(value = 15, message = "Kilo en az 15 kg olmalidir")
        @Max(value = 500, message = "Kilo en fazla 500 kg olabilir")
        Integer weightKg,

        @Pattern(regexp = "^(?i)(LEFT|RIGHT|BOTH)?$", message = "Dominant ayak LEFT, RIGHT veya BOTH olmalidir")
        String dominantFoot,

        @Size(max = 2000, message = "Hakkinda alani en fazla 2000 karakter olabilir")
        String bio,

        @Pattern(regexp = "^https?://[^\\s]+$", message = "Avatar URL gecerli bir http(s) adresi olmalidir")
        String avatarUrl

) {}