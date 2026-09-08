package com.routeiq.device.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "segment_historical_stats",
        indexes = {
                @Index(columnList = "routeId, segmentStartDistance, segmentEndDistance, hourOfDay, dayOfWeek")
        })
public class SegmentHistoricalStatsEntity {
    @Id
    @GeneratedValue
    private Long id;

    private Long routeId;
    private Double segmentStartDistance;
    private Double segmentEndDistance;
    private Integer hourOfDay;   // 0-23
    private Integer dayOfWeek;   // 1-7 (or use DayOfWeek enum ordinal)

    private Double meanTime;     // seconds
    private Double variance;     // seconds^2
    private Integer sampleCount; // how many observations went into this stat
}
