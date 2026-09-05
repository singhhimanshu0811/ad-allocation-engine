package com.routeiq.device.model;

import jakarta.validation.constraints.NotBlank;

public record DeviceRequest(
        @NotBlank String deviceId
) {}
