package com.routeiq.device.entity;

import jakarta.persistence.*;

import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "heartbeats", indexes = {
        @Index(name = "idx_heartbeats_device_received_at", columnList = "device_id,received_at")
})
public class HeartbeatEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private DeviceCredentialEntity device;

    @Column(nullable = false)
    private boolean heartbeat;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected HeartbeatEntity() {
    }

    public HeartbeatEntity(DeviceCredentialEntity device, boolean heartbeat, Instant receivedAt) {
        this.device = device;
        this.heartbeat = heartbeat;
        this.receivedAt = receivedAt;
        this.expiresAt = receivedAt.plus(Duration.ofDays(7));
    }
}
