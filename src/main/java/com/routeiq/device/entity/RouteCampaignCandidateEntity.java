package com.routeiq.device.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.util.RouteMatcher;

@Data
@Entity
@Table(name = "route_campaign_candidates",
        indexes = {
                @Index(columnList = "route_id"),
                @Index(columnList = "campaign_id"),
                @Index(columnList = "route_id, campaign_id"),
        })
public class RouteCampaignCandidateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false, foreignKey = @ForeignKey(name = "fk_route_candidate"))
    private RouteEntity route;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false, foreignKey = @ForeignKey(name = "fk_campaign_candidate"))
    private Campaign campaign;

    @Column(name = "entry_marker", nullable = false)
    private Double entryMarker;

    @Column(name = "exit_marker", nullable = false)
    private Double exitMarker;

    @Column(name = "distance_to_campaign_center_meters", nullable = false)
    private Double distanceOfEntryMarkerFromCenter;

    @Column(name = "active", nullable = false)
    private Boolean active;
}
