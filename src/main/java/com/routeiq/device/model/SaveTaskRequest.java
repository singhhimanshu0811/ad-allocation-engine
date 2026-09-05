package com.routeiq.device.model;

import jakarta.validation.constraints.NotBlank;

public record SaveTaskRequest(
        @NotBlank String deviceId,
        @NotBlank String task
) {
}
