package com.routeiq.device.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;

import java.sql.Timestamp;
import org.locationtech.jts.geom.Point;

@Table(name = "device_current_state")
@Entity
public class DeviceCurrentStateEntity {

    @Id
    @Column(name = "device_id", nullable = false, length = 16)
    private String deviceId;

    @Column(name = "current_route_id", nullable = false, length = 16)
    private String currentRouteId;

    @Column(name = "position", nullable = false, columnDefinition = "geography(Point,4326)")
    private Point position;

    @Column(name = "distance_along_route", nullable = false)
    private Double distanceAlongRoute;

    @Column(name = "smoothed_delay", nullable = false)
    private Double smoothedDelay;

    @Column(name = "error_covariance", nullable = false)
    private Double errorCovariance;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    @Column(name = "updated_at", nullable = false)
    private Timestamp updatedAt;
}
