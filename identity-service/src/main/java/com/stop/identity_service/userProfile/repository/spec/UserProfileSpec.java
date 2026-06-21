package com.stop.identity_service.userProfile.repository.spec;

import com.stop.identity_service.userProfile.entity.profile.UserProfile;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Locale;

public final class UserProfileSpec {

    private UserProfileSpec() {}

    public static Specification<UserProfile> withFetch() {
        return (root, query, cb) -> {
            if (query.getResultType() == UserProfile.class) {
                root.fetch("user", JoinType.INNER);
            }
            return cb.conjunction();
        };
    }

    public static Specification<UserProfile> displayNameLike(String displayName) {
        if (displayName == null || displayName.isBlank()) return alwaysTrue();
        String v = "%" + displayName.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, cq, cb) -> cb.like(cb.lower(root.get("user").get("displayName")), v);
    }

    public static Specification<UserProfile> cityEq(String city) {
        if (city == null || city.isBlank()) return alwaysTrue();
        String v = city.trim().toLowerCase(Locale.ROOT);
        return (root, cq, cb) -> cb.equal(cb.lower(root.get("city")), v);
    }

    public static Specification<UserProfile> positionEq(String position) {
        if (position == null || position.isBlank()) return alwaysTrue();
        String v = position.trim().toLowerCase(Locale.ROOT);
        return (root, cq, cb) -> cb.equal(cb.lower(root.get("position")), v);
    }

    public static Specification<UserProfile> dominantFootEq(String dominantFoot) {
        if (dominantFoot == null || dominantFoot.isBlank()) return alwaysTrue();
        String v = dominantFoot.trim().toUpperCase(Locale.ROOT);
        return (root, cq, cb) -> cb.equal(cb.upper(root.get("dominantFoot")), v);
    }

    public static Specification<UserProfile> heightBetween(Integer min, Integer max) {
        Specification<UserProfile> spec = alwaysTrue();
        if (min != null) spec = spec.and((root, cq, cb) -> cb.greaterThanOrEqualTo(root.get("heightCm"), min));
        if (max != null) spec = spec.and((root, cq, cb) -> cb.lessThanOrEqualTo(root.get("heightCm"), max));
        return spec;
    }

    public static Specification<UserProfile> weightBetween(Integer min, Integer max) {
        Specification<UserProfile> spec = alwaysTrue();
        if (min != null) spec = spec.and((root, cq, cb) -> cb.greaterThanOrEqualTo(root.get("weightKg"), min));
        if (max != null) spec = spec.and((root, cq, cb) -> cb.lessThanOrEqualTo(root.get("weightKg"), max));
        return spec;
    }

    public static Specification<UserProfile> trustScoreBetween(BigDecimal min, BigDecimal max) {
        Specification<UserProfile> spec = alwaysTrue();
        if (min != null) spec = spec.and((root, cq, cb) -> cb.greaterThanOrEqualTo(root.get("user").get("trustScore"), min));
        if (max != null) spec = spec.and((root, cq, cb) -> cb.lessThanOrEqualTo(root.get("user").get("trustScore"), max));
        return spec;
    }

    public static Specification<UserProfile> rankScoreBetween(BigDecimal min, BigDecimal max) {
        Specification<UserProfile> spec = alwaysTrue();
        if (min != null) spec = spec.and((root, cq, cb) -> cb.greaterThanOrEqualTo(root.get("user").get("rankScore"), min));
        if (max != null) spec = spec.and((root, cq, cb) -> cb.lessThanOrEqualTo(root.get("user").get("rankScore"), max));
        return spec;
    }

    private static Specification<UserProfile> alwaysTrue() {
        return (root, cq, cb) -> cb.conjunction();
    }
}
