package com.routeiq.device.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "geo_locations", indexes = {
        @Index(name = "idx_geo_locations_route_generated_at", columnList = "route_id,generated_at")
})
@NoArgsConstructor
@Data
public class GeoLocationEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "capture_session_id", nullable = false)
    private UUID captureSessionId;

    @Column(name = "sequence_number", nullable = false)
    private Long sequenceNumber;

    @Column(nullable = false)
    private double lat;

    @Column(nullable = false)
    private double lan;

    @Column(name = "generated_at", nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Timestamp generatedAt;


}
