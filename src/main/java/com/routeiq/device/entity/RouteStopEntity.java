package com.routeiq.device.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.time.LocalTime;

@Table(name = "route_stops",
    indexes = {
        @Index(name= "idx_route_stops_route_id", columnList = "route_id"),
            @Index(name = "idx_route_id_and_sequence_number", columnList = "route_id, stop_sequence_number")
})
@Entity
@Data
public class RouteStopEntity {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long routeStopId;

    @Column(name = "route_id", nullable = false)
    private Long routeId;
    @Column(name = "stop_sequence_number", nullable = false)
    private Integer stopSequenceNumber;

    @Column(columnDefinition = "geography(Point,4326)", nullable = false)
    private Point stopPoint;

    @Column(name = "distance_along_route", nullable = false)
    private Double distanceAlongRoute;

    @Column(name = "scheduled_time", nullable = false)
    private Instant scheduledTime;
}
