package com.routeiq.device.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import org.geolatte.geom.LineString;
import java.sql.Timestamp;

import java.util.UUID;

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

    @Column(name = "device_id", nullable = false, length = 16)
    private String deviceId;

    @Column(name = "route_id", nullable = false, length = 16)
    private String routeId;

    @Column(name = "from_location", nullable = false, length = 120)
    private String fromLocation;

    @Column(name = "to_location", nullable = false, length = 120)
    private String toLocation;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    @Column(name = "start_time", nullable = false)
    private Timestamp startTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    @Column(name = "end_time", nullable = false)
    private Timestamp endTime;

    @Column(name = "is_active", nullable = false)
    private boolean active;



}
