package com.stop.identity_service.user.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record UserProfileRequest(

        @NotBlank(message = "Ad zorunludur")
        @Size(max = 50, message = "Ad en fazla 50 karakter olabilir")
        @Pattern(
                regexp = "^[\\p{L}\\p{M}\\s'.-]+$",
                message = "Ad gecersiz karakterler iceriyor"
        )
        String firstName,

        @NotBlank(message = "Soyad zorunludur")
        @Size(max = 50, message = "Soyad en fazla 50 karakter olabilir")
        @Pattern(
                regexp = "^[\\p{L}\\p{M}\\s'.-]+$",
                message = "Soyad gecersiz karakterler iceriyor"
        )
        String lastName,

        @NotNull(message = "Dogum tarihi zorunludur")
        @Past(message = "Dogum tarihi gecmiste olmalidir")
        LocalDate birthDate,

        @NotBlank(message = "Sehir zorunludur")
        @Size(max = 100, message = "Sehir en fazla 100 karakter olabilir")
        String city,

        @NotBlank(message = "Pozisyon zorunludur")
        @Size(max = 100, message = "Pozisyon en fazla 100 karakter olabilir")
        String position,

        @NotNull(message = "Boy zorunludur")
        @Min(value = 50, message = "Boy en az 50 cm olmalidir")
        @Max(value = 280, message = "Boy en fazla 280 cm olabilir")
        Integer heightCm,

        @NotNull(message = "Kilo zorunludur")
        @Min(value = 15, message = "Kilo en az 15 kg olmalidir")
        @Max(value = 500, message = "Kilo en fazla 500 kg olabilir")
        Integer weightKg,

        @NotBlank(message = "Dominant ayak secimi zorunludur")
        @Pattern(
                regexp = "^(?i)(LEFT|RIGHT|BOTH)$",
                message = "Dominant ayak LEFT, RIGHT veya BOTH olmalidir"
        )
        String dominantFoot,

        @Size(max = 2000, message = "Hakkinda alani en fazla 2000 karakter olabilir")
        String bio,

        @Size(max = 512, message = "Avatar URL en fazla 512 karakter olabilir")
        @Pattern(
                regexp = "^https?://[^\\s]+$",
                message = "Avatar URL gecerli bir http(s) adresi olmalidir"
        )
        String avatarUrl

) {}