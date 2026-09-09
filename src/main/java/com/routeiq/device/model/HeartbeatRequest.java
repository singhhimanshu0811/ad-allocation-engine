package com.routeiq.device.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record HeartbeatRequest(
        @NotBlank String deviceId,
        @NotNull Boolean heartbeat,
        @NotNull Double lat,
        @NotNull Double lon,
        @NotNull Instant pingTime
) {}
