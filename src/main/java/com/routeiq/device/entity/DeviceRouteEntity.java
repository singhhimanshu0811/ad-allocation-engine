package com.routeiq.device.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "devices_routes")
public class DeviceRouteEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 16)
    private String deviceId;

    @Column(name = "from_location", nullable = false, length = 120)
    private String fromLocation;

    @Column(name = "to_location", nullable = false, length = 120)
    private String toLocation;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    protected DeviceRouteEntity() {
    }

    public DeviceRouteEntity(String deviceId, String fromLocation, String toLocation, boolean active) {
        this.deviceId = deviceId;
        this.fromLocation = fromLocation;
        this.toLocation = toLocation;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
