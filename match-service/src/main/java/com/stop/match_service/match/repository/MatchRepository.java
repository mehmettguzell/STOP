package com.stop.match_service.match.repository;

import com.stop.match_service.match.entity.Match;
import com.stop.match_service.match.entity.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchRepository extends JpaRepository<Match, UUID>, JpaSpecificationExecutor<Match> {

    @Override
    Optional<Match> findById(UUID uuid);

    List<Match> findAllByStatusAndRatingDeadlineBefore(Status status, LocalDateTime deadline);

    @Modifying
    @Query("UPDATE Match m SET m.status = :status WHERE m.id IN :ids")
    void updateStatusByIds(@Param("ids") List<UUID> ids, @Param("status") Status status);
}