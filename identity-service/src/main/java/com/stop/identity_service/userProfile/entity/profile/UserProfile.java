package com.stop.identity_service.userProfile.entity.profile;

import com.stop.identity_service.userProfile.dto.request.UpdateUserProfileRequest;
import com.stop.identity_service.user.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @Column(name = "user_id")
    private UUID userId;


    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    private String city;

    private String position;

    @Column(name = "height_cm")
    private Integer heightCm;

    @Column(name = "weight_kg")
    private Integer weightKg;

    @Column(name = "dominant_foot")
    private String dominantFoot;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;


    public boolean applyPatch(UpdateUserProfileRequest req) {
        boolean changed = false;
        if (req.firstName() != null && !Objects.equals(req.firstName(), firstName)) { firstName = req.firstName(); changed = true; }
        if (req.lastName() != null && !Objects.equals(req.lastName(), lastName)) { lastName = req.lastName(); changed = true; }
        if (req.birthDate() != null && !Objects.equals(req.birthDate(), birthDate)) { birthDate = req.birthDate(); changed = true; }
        if (req.city() != null && !Objects.equals(req.city(), city)) { city = req.city(); changed = true; }
        if (req.position() != null && !Objects.equals(req.position(), position)) { position = req.position(); changed = true; }
        if (req.heightCm() != null && !Objects.equals(req.heightCm(), heightCm)) { heightCm = req.heightCm(); changed = true; }
        if (req.weightKg() != null && !Objects.equals(req.weightKg(), weightKg)) { weightKg = req.weightKg(); changed = true; }
        if (req.dominantFoot() != null && !Objects.equals(req.dominantFoot(), dominantFoot)) { dominantFoot = req.dominantFoot(); changed = true; }
        if (req.bio() != null) {
            String v = req.bio().isBlank() ? null : req.bio();
            if (!Objects.equals(v, bio)) { bio = v; changed = true; }
        }
        return changed;
    }

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}