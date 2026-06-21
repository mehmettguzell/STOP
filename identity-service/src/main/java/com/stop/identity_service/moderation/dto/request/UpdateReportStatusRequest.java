package com.stop.identity_service.moderation.dto.request;

import com.stop.identity_service.moderation.entity.ReportStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateReportStatusRequest(
        @NotNull ReportStatus status
) {}
