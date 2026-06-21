package com.stop.communication_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "poll_options", indexes = @Index(name = "idx_poll_options_poll", columnList = "poll_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PollOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    @Column(nullable = false, length = 200)
    private String text;

    @Column(nullable = false)
    private int position;
}
