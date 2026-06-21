package com.stop.identity_service.userProfile.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(

        @Email(message = "Gecersiz e-posta formati")
        String email,

        @Size(min = 3, max = 30,
                message = "Kullanici adi 3 ile 30 karakter arasinda olmalidir")
        String displayName,

        @Pattern(
                regexp = "^\\+[1-9][0-9]{7,14}$",
                message = "Telefon numarasi uluslararasi formatta olmalidir (orn. +905xxxxxxxxx)"
        )
        String phoneNumber,

        @Size(min = 8, max = 64,
                message = "Sifre 8 ile 64 karakter arasinda olmalidir")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).*$",
                message = "Sifre en az bir buyuk harf, bir kucuk harf ve bir rakam icermelidir"
        )
        String password,

        String rePassword

) {}