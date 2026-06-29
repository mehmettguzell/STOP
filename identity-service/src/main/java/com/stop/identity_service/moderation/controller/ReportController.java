package com.stop.identity_service.moderation.controller;

import com.stop.identity_service.config.jwt.SecurityUtils;
import com.stop.identity_service.moderation.dto.request.ReportRequest;
import com.stop.identity_service.moderation.dto.request.ResolveAndRejectReportRequest;
import com.stop.identity_service.moderation.dto.request.UpdateReportStatusRequest;
import com.stop.identity_service.moderation.dto.response.ReportDetailResponse;
import com.stop.identity_service.moderation.dto.response.ReportResponse;
import com.stop.identity_service.moderation.entity.ReportStatus;
import com.stop.identity_service.moderation.service.ReportService;
import com.stop.identity_service.userProfile.dto.response.SliceResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService service;

    @PostMapping
    public ResponseEntity<ReportResponse> sendReport(@Valid @RequestBody ReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.sendReport(request, SecurityUtils.getCurrentUserId()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SliceResponse<ReportResponse>> getReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getReports(status, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportDetailResponse> getReport(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getReport(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportResponse> updateReportStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReportStatusRequest request) {
        return ResponseEntity.ok(service.updateReportStatus(id, request.status()));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportResponse> resolveReport(
            @PathVariable UUID id,
            @RequestBody ResolveAndRejectReportRequest request) {
        return ResponseEntity.ok(service.resolveReport(id, SecurityUtils.getCurrentUserId(), request.note()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportResponse> rejectReport(
            @PathVariable UUID id,
            @RequestBody ResolveAndRejectReportRequest request) {
        return ResponseEntity.ok(service.rejectReport(id, SecurityUtils.getCurrentUserId(), request.note()));
    }


}
