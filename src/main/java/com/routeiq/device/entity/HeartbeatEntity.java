package com.routeiq.device.entity;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "heartbeats", indexes = {
        @Index(name = "idx_heartbeats_device_received_at", columnList = "device_id,received_at")
})
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class HeartbeatEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 16)
    private String deviceId;

    @Column(name = "route_id", nullable = false)
    private Long routeId;

    @Column(name = "distance_along_route", nullable = false)
    private double distanceAlongRoute;

    @Column(nullable = false)
    private boolean heartbeat;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

}
