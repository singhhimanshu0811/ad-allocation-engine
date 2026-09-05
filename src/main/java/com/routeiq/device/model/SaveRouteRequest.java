package com.routeiq.device.model;

import jakarta.validation.constraints.NotBlank;

public record SaveRouteRequest(
        @NotBlank String deviceId,
        @NotBlank String fromLocation,
        @NotBlank String toLocation
) {
}
