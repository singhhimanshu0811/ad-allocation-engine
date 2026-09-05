package com.routeiq.device.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GetImagesRequest(
        @NotBlank String deviceId,
        @NotNull @Valid GeoLocation location
) {}
