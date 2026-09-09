package com.routeiq.device.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record SaveGeoLocationsPingRequest(
        @NotBlank String deviceId, //to remember whichever device captured this route pings
        @NotNull UUID captureSessionId,
        @NotNull Long sequenceNumber,//last += 1
        @NotEmpty List<@Valid GeoLocation> locations
        ) {}
