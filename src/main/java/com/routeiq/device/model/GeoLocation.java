package com.routeiq.device.model;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record GeoLocation(
        @NotNull Double lat,
        @NotNull Double lang,
        Double speed,
        Double heading,
        @NotNull Instant generatedAt) {
}
