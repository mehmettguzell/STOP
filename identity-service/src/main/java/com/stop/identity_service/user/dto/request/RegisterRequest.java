package com.stop.identity_service.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "E-posta bos olamaz")
        @Email(message = "Gecersiz e-posta formati")
        String email,

        @NotBlank(message = "Telefon numarasi bos olamaz")
        @Pattern(
                regexp = "^\\+[1-9][0-9]{7,14}$",
                message = "Telefon numarasi uluslararasi formatta olmalidir (orn. +905551234567)"
        )
        String phoneNumber,

        @NotBlank(message = "Sifre bos olamaz")
        @Size(min = 8, max = 64,
                message = "Sifre 8 ile 64 karakter arasinda olmalidir")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).*$",
                message = "Sifre en az bir buyuk harf, bir kucuk harf ve bir rakam icermelidir"
        )
        String password,

        @NotBlank(message = "Kullanici adi bos olamaz")
        @Size(min = 2, max = 20,
                message = "Kullanici adi 2 ile 20 karakter arasinda olmalidir")
        String displayName
) {}