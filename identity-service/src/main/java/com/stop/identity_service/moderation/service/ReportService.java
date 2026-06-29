package com.stop.identity_service.moderation.service;

import com.stop.identity_service.common.error.IdentityErrorCode;
import com.stop.identity_service.common.exception.BusinessException;
import com.stop.identity_service.moderation.dto.request.ReportRequest;
import com.stop.identity_service.moderation.dto.response.ReportDetailResponse;
import com.stop.identity_service.moderation.dto.response.ReportResponse;
import com.stop.identity_service.moderation.entity.Report;
import com.stop.identity_service.moderation.entity.ReportStatus;
import com.stop.identity_service.moderation.repository.ReportRepository;
import com.stop.identity_service.userProfile.dto.response.SliceResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final ReportRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    // TODO: Eventt yayınlanabilir -> Notification Service


    /*
     * =========================
     * WRITE OPERATIONS
     * =========================
     */

    @Transactional
    public ReportResponse sendReport(ReportRequest request, UUID reporterId) {
        isEligibleToReport(reporterId, request.targetUserId());
        Report report = mapToReport(request, reporterId);
        Report saved = repository.save(report);
        return mapToReportResponse(saved);
    }

    @Transactional
    public ReportResponse updateReportStatus(UUID id, ReportStatus newStatus) {
        Report report = findById(id);

        if (report.getStatus() != ReportStatus.OPEN || newStatus != ReportStatus.REVIEWING) {
            throw new BusinessException(IdentityErrorCode.INVALID_STATUS_TRANSITION);
        }

        report.setStatus(newStatus);
        return mapToReportResponse(repository.save(report));
    }

    @Transactional
    public ReportResponse resolveReport(UUID id, UUID adminId, String note) {
        Report report = findById(id);

        if (report.getStatus() == ReportStatus.RESOLVED || report.getStatus() == ReportStatus.REJECTED) {
            throw new BusinessException(IdentityErrorCode.REPORT_ALREADY_CLOSED);
        }

        report.setStatus(ReportStatus.RESOLVED);
        report.setResolvedBy(adminId);
        report.setResolvedAt(Instant.now());
        report.setResolveNote(note);
        return mapToReportResponse(repository.save(report));
    }

    @Transactional
    public ReportResponse rejectReport(UUID id, UUID adminId, String note) {
        Report report = findById(id);

        if (report.getStatus() == ReportStatus.RESOLVED || report.getStatus() == ReportStatus.REJECTED) {
            throw new BusinessException(IdentityErrorCode.REPORT_ALREADY_CLOSED);
        }

        report.setStatus(ReportStatus.REJECTED);
        report.setResolvedBy(adminId);
        report.setResolvedAt(Instant.now());
        report.setResolveNote(note);
        return mapToReportResponse(repository.save(report));
    }

    /*
     * =========================
     * READ OPERATIONS
     * =========================
     */

    @Transactional(readOnly = true)
    public SliceResponse<ReportResponse> getReports(ReportStatus status, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return SliceResponse.of(
                status != null
                        ? repository.findAllByStatusOrderByCreatedAtDesc(status, pageRequest).map(this::mapToReportResponse)
                        : repository.findAllByOrderByCreatedAtDesc(pageRequest).map(this::mapToReportResponse)
        );
    }

    @Transactional(readOnly = true)
    public @Nullable ReportDetailResponse getReport(UUID id) {
        Report report = findById(id);
        return mapToReportDetailResponse(report);
    }

    /*
     * =========================
     * PRIVATE — HELPERS
     * =========================
     */
    private Report findById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new BusinessException(IdentityErrorCode.REPORT_NOT_FOUND));
    }


    /*
     * =========================
     * PRIVATE — VALIDATIONS
     * =========================
     */

    private void isEligibleToReport(UUID reporterId, UUID targetUserId) {
        List<ReportStatus> statuses = List.of(ReportStatus.OPEN, ReportStatus.REVIEWING);

        if (reporterId.equals(targetUserId)) {
            throw new BusinessException(IdentityErrorCode.CANNOT_REPORT_YOURSELF);
        }
        if (repository.existsActiveReport(reporterId, targetUserId,statuses)){
            throw new BusinessException(IdentityErrorCode.REPORT_ALREADY_SENT);
        }
    }

    /*
     * =========================
     * PRIVATE — MAPPERS
     * =========================
     */

    private Report mapToReport(ReportRequest req, UUID reporterID) {
        return Report.builder()
                .reporterId(reporterID)
                .targetUserId(req.targetUserId())
                .matchId(req.matchId())
                .reason(req.reason())
                .build();
    }

    private ReportResponse mapToReportResponse(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getReporterId(),
                report.getTargetUserId(),
                report.getMatchId(),
                report.getReason(),
                report.getStatus(),
                report.getCreatedAt()
        );
    }

    private ReportDetailResponse mapToReportDetailResponse(Report report) {
        return new ReportDetailResponse(
                report.getId(),
                report.getReporterId(),
                report.getTargetUserId(),
                report.getMatchId(),
                report.getReason(),
                report.getStatus(),
                report.getCreatedAt(),
                report.getResolvedBy(),
                report.getResolvedAt(),
                report.getResolveNote()
        );
    }



}
