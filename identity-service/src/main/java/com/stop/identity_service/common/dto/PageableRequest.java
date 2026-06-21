package com.stop.identity_service.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PageableRequest(
        @Min(value = 0, message = "Sayfa numarası 0'dan küçük olamaz")
        Integer page,

        @Min(value = 1, message = "Sayfa boyutu en az 1 olmalıdır")
        @Max(value = 100, message = "Sayfa boyutu en fazla 100 olabilir")
        Integer size
) {
    public PageableRequest {
        if (page == null) page = 0;
        if (size == null) size = 20;
    }

    public org.springframework.data.domain.PageRequest toPageRequest() {
        return org.springframework.data.domain.PageRequest.of(page, size);
    }
}
