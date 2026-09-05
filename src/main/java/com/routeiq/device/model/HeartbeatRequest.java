package com.routeiq.device.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record HeartbeatRequest(
        @NotBlank String deviceId,
        @NotNull Boolean heartbeat
) {}
