package com.routeiq.device.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Table(name = "hourly_allocation",
        indexes = {
                @Index(columnList = "device_id, start_window, end_window"),
        })
@Setter
@Getter
public class PeriodicAllocationEntity {

    @Column(name = "id", nullable = false)
    @GeneratedValue
    @Id
    Long id;

    @Column(name = "device_id", nullable = false)
    String deviceId;
    @Column(name = "campaign_id", nullable = false)
    String campaignId;

    @Column(name = "start_window", nullable = false)
    Instant startWindow;

    @Column(name = "end_window", nullable = false)
    Instant endWindow;

    @Column
    LocalDate dateOfAllocation;

    @Column
    LocalTime localStartTime;

    @Column
    LocalTime localEndTime;

    @Column(name = "allocated_plays", nullable = false)
    int allocatedPlays;

}
