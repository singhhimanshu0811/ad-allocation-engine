package com.routeiq.device.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(
        name = "segment_historical_stats",
        indexes = {
                @Index(
                        name = "idx_segment_historical_stats_route_segment_hour_day",
                        columnList = "route_id, segment_start_distance, segment_end_distance, hour_of_day, day_of_week"
                )
        }
)
public class SegmentHistoricalStatsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_id")
    private Long routeId;

    @Column(name = "segment_start_distance")
    private Double segmentStartDistance;

    @Column(name = "segment_end_distance")
    private Double segmentEndDistance;

    @Column(name = "hour_of_day")
    private Integer hourOfDay;   // 0-23

    @Column(name = "day_of_week")
    private Integer dayOfWeek;   // 1-7

    @Column(name = "mean_time")
    private Double meanTime;     // seconds

    @Column(name = "variance")
    private Double variance;     // seconds^2

    @Column(name = "sample_count")
    private Integer sampleCount; // number of observations
}

