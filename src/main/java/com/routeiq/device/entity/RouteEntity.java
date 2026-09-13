package com.routeiq.device.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.LineString;

@Table(name = "routes", indexes = {
        @Index(name = "idx_routes_path", columnList = "path")
})
@Entity
@Setter
@Getter
public class RouteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long routeId;

    @Column(columnDefinition = "geometry(LineString, 4326)")
    private LineString path;

    @Column(name = "route_name", nullable = false, length = 128)
    private String routeName;
}
