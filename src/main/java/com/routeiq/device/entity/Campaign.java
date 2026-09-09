package com.routeiq.device.entity;

import com.routeiq.device.state.CampaignState;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
@Getter
@Setter
@Entity
@Table(
        name = "campaigns",
        indexes = {
                @Index(name = "idx_campaigns_start_date", columnList = "start_date"),
                @Index(name = "idx_campaigns_end_date", columnList = "end_date"),
                @Index(name = "idx_campaigns_status", columnList = "status")
        }
)
public class Campaign {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "advertiser_id", nullable = false)
    private Advertiser advertiser;

    @Column(name = "advertiser_email", nullable = false)
    private String advertiserEmail;

    private String name;

    private String description;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CampaignState state = CampaignState.DRAFT;
    private String creativeFileName;
    private String creativeUrl;
    private Instant startInstant;
    private Instant endInstant;
    private Double latitude;
    private Double longitude;
    private Double radiusKm;
    private Double budget;
    private Integer impressions;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
