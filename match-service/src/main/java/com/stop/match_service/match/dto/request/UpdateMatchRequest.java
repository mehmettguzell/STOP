package com.stop.match_service.match.dto.request;

import com.stop.match_service.match.entity.Visibility;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record UpdateMatchRequest(

    @Size(max = 150, message = "Baslik en fazla 150 karakter olabilir")
    String title,

    @Size(max = 5000, message = "Aciklama cok uzun")
    String description,

    @Size(max = 255, message = "Konum en fazla 255 karakter olabilir")
    String location,

    @Future(message = "Baslangic zamani gelecekte olmalidir")
    LocalDateTime startTime,

    Visibility visibility,

    @Min(value = 2, message = "Kapasite en az 2 olmalidir")
    Integer capacity
) {
}


