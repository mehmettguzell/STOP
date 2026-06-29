package com.stop.identity_service.moderation.repository;

import com.stop.identity_service.moderation.entity.Report;
import com.stop.identity_service.moderation.entity.ReportStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    Slice<Report> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Slice<Report> findAllByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    @Query("""
        SELECT count(r) > 0 
            from Report r
            WHERE r.reporterId = :reporterId
              and r.targetUserId = :targetUserId
              and r.status IN :statuses
    """)
    boolean existsActiveReport(
            @Param("reporterId") UUID reporterId,
            @Param("targetUserId") UUID targetUserId,
            @Param("statuses") List<ReportStatus> statuses
    );
}
