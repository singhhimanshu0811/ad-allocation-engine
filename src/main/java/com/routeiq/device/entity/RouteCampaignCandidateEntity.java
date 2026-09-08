package com.routeiq.device.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "route_campaign_candidates",
        indexes = {
                @Index(columnList = "routeId"),
                @Index(columnList = "campaignId")
        })
public class RouteCampaignCandidateEntity {
    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "route_id", nullable = false)
    private Long routeId;

    @Column(name = "campaign_id", nullable = false)
    private String campaignId;

    @Column(name = "entry_marker", nullable = false)
    private Double entryMarker;

    @Column(name = "exit_marker", nullable = false)
    private Double exitMarker;

    @Column(name = "active", nullable = false)
    private Boolean active;
}
