package com.routeiq.device.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SaveGeoLocationsRequest(
        @NotBlank String deviceId,
        @NotNull Long routeId,
        @NotEmpty List<@Valid GeoLocation> locations
) {}
