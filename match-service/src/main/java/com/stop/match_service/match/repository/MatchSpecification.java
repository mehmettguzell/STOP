package com.stop.match_service.match.repository;

import com.stop.match_service.match.entity.Match;
import com.stop.match_service.match.entity.Status;
import com.stop.match_service.match.entity.Visibility;
import com.stop.match_service.matchParticipation.entity.MatchParticipant;
import com.stop.match_service.matchParticipation.entity.ParticipantStatus;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MatchSpecification {

    public static Specification<Match> search(String title, String location, LocalDate date, Status status, Visibility visibility, UUID excludeUserId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (title != null && !title.isBlank()) {
                String escaped = title.toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\")
                        .replace("%", "\\%")
                        .replace("_", "\\_");
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + escaped + "%", '\\'));
            }

            if (location != null && !location.isBlank()) {
                String escaped = location.toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\")
                        .replace("%", "\\%")
                        .replace("_", "\\_");
                predicates.add(cb.like(cb.lower(root.get("location")), "%" + escaped + "%", '\\'));
            }

            if (date != null) {
                LocalDateTime startOfDay = date.atStartOfDay();
                LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
                predicates.add(cb.between(root.get("startTime"), startOfDay, endOfDay));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            } else {
                predicates.add(root.get("status").in(Status.CREATED, Status.OPEN, Status.FULL));
            }

            if (visibility != null) {
                predicates.add(cb.equal(root.get("visibility"), visibility));
            }

            if (excludeUserId != null) {
                Subquery<UUID> sub = query.subquery(UUID.class);
                var mp = sub.from(MatchParticipant.class);
                sub.select(mp.get("match").get("id"))
                        .where(
                                cb.equal(mp.get("userId"), excludeUserId),
                                mp.get("status").in(ParticipantStatus.JOINED)
                        );
                predicates.add(cb.not(root.get("id").in(sub)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}