package com.routeiq.device.model;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record AllocationTimeIntervalModel(@NotNull LocalDate startDate,
                                            @NotNull LocalDate endDate,

                                            @NotNull LocalTime startTime,
                                          @NotNull LocalTime endTime) {

}
