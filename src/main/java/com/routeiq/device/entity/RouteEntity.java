package com.routeiq.device.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.locationtech.jts.geom.LineString;

@Table(name = "routes")
@Entity
@Data
public class RouteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long routeId;

    @Column(columnDefinition = "geography(LineString, 4326)")
    private LineString path;

    @Column(name = "route_name", nullable = false, length = 128)
    private String routeName;
}
