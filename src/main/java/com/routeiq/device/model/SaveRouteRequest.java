package com.routeiq.device.model;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public record SaveRouteRequest(
        @NotBlank UUID captureSessionId,
        @NotBlank String routeName
) {
}
