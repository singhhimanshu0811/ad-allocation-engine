package com.routeiq.device.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.routeiq.device.model.GeoLocation;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "geo_locations", indexes = {
        @Index(name = "idx_geo_locations_device_capture_session", columnList = "device_id,capture_session_id"),
        @Index(name = "capture_session_id", columnList = "capture_session_id")
})
@NoArgsConstructor
@Data
public class GeoLocationEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 16)
    private String deviceId;

    @Column(name = "capture_session_id", nullable = false)
    private UUID captureSessionId;

    @Column(name = "sequence_number", nullable = false)
    private Long sequenceNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "positions")
    private String positions;


    @Column(name = "generated_at", nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant generatedAt;


}
