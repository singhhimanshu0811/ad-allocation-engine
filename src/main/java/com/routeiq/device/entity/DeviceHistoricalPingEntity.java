package com.routeiq.device.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;

import java.sql.Timestamp;

@Table(name = "gps_ping_log")
@Entity
public class DeviceHistoricalPingEntity {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 16)
    private String deviceId;

    @Column(name = "route_id", nullable = false)
    private Long routeId;

    @Column(name = "distance_along_route", nullable = false)
    private double distanceAlongRoute;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    @Column(name = "timestamp", nullable = false)
    private Timestamp timestamp;
}


