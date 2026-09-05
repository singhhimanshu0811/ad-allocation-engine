package com.routeiq.device.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "geo_locations", indexes = {
        @Index(name = "idx_geo_locations_route_generated_at", columnList = "route_id,generated_at")
})
public class GeoLocationEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private DeviceRouteEntity route;

    @Column(nullable = false)
    private double lat;

    @Column(nullable = false)
    private double lang;

    private Double speed;

    private Double heading;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    protected GeoLocationEntity() {
    }

    public GeoLocationEntity(DeviceRouteEntity route, double lat, double lang, Double speed, Double heading,
                             Instant generatedAt) {
        this.route = route;
        this.lat = lat;
        this.lang = lang;
        this.speed = speed;
        this.heading = heading;
        this.generatedAt = generatedAt;
    }
}
