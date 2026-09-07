package com.routeiq.device.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.routeiq.device.model.GeoLocation;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.Instant;
import java.util.List;
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


    @Column(columnDefinition = "jsonb")
    private String positions;


    @Column(name = "generated_at", nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant generatedAt;


}
