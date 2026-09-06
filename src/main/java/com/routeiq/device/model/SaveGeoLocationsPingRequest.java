package com.routeiq.device.model;

import jakarta.validation.constraints.NotBlank;

import lombok.Builder;

import java.util.UUID;

@Builder
public record SaveGeoLocationsPingRequest(
        @NotBlank String deviceId, //to remember whichever device captured this route pings
        @NotBlank UUID captureSessionId,
        @NotBlank Long sequenceNumber,//last += 1
        @NotBlank double lat,
        @NotBlank double lan
        ) {}
