package com.routeiq.device.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "heartbeats", indexes = {
        @Index(name = "idx_heartbeats_device_received_at", columnList = "device_id,received_at"),
        @Index(name = "idx_heartbeats_device_on_route", columnList = "device_id,route_id")
})
@NoArgsConstructor
@Setter
@Getter
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


}
