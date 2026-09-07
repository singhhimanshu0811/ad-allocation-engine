package com.routeiq.device.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record HeartbeatRequest(
        @NotBlank String deviceId,
        @NotNull Boolean heartbeat,
        @NotBlank double lat,
        @NotBlank double lon,
        @NotBlank Instant pingTime
        ) {}
