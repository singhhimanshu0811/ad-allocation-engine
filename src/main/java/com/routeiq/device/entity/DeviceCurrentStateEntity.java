package com.routeiq.device.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

@Table(name = "device_current_state",
indexes = {
                @Index(columnList = "device_id"),

        })
@Entity
@Setter
@Getter
public class DeviceCurrentStateEntity {

    @Id
    @Column(name = "device_id", nullable = false, length = 16)
    private String deviceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "current_route_id", nullable = false, foreignKey = @ForeignKey(name = "fk_device_current_route"))
    private RouteEntity currentRouteId;

    @Column(name = "position", nullable = false, columnDefinition = "geography(Point,4326)")
    private Point position;

    @Column(name = "distance_along_route", nullable = false)
    private Double distanceAlongRoute;


    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
