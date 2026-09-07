package com.routeiq.device.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;


import java.time.Instant;


@Entity
@Table(name = "devices_routes")
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
    private DeviceCredentialEntity deviceId;

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
