package com.stop.identity_service.friendship.dto.response;
import org.springframework.data.domain.Slice;

import java.util.List;

public record SliceResponse<T>(
        List<T> content,
        int page,
        int size,
        boolean hasNext
) {
    public static <T> SliceResponse<T> of(Slice<T> slice) {
        return new SliceResponse<>(
                slice.getContent(),
                slice.getNumber(),
                slice.getNumberOfElements(),
                slice.hasNext()
        );
    }
}