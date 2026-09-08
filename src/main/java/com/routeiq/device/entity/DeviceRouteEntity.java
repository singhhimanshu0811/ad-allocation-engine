package com.routeiq.device.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "devices_routes",
        indexes = {
                @Index(name = "idx_devices_routes_device_id", columnList = "device_id"),
                @Index(name = "idx_devices_routes_route_id", columnList = "route_id"),
                @Index(name = "idx_devices_routes_device_route", columnList = "device_id, route_id"),
                @Index(name = "idx_devices_routes_route_active", columnList = "route_id, is_active"),
                @Index(name = "idx_devices_routes_device_active", columnList = "device_id, is_active")
        })
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceRouteEntity extends AuditableEntity {

    @Id
    @GeneratedValue
    private Long id;
    //one assignment of device to a route, a device can have multiple assignments to different routes over time

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private DeviceCredentialEntity device;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private RouteEntity route;

    @Column(name = "from_location", nullable = false, length = 120)
    private String fromLocation;

    @Column(name = "to_location", nullable = false, length = 120)
    private String toLocation;

    @Column(name = "is_active", nullable = false)
    private boolean active;



}
