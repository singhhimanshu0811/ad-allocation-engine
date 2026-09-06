package com.routeiq.device.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Builder
public record SaveGeoLocationsPingRequest(
        @NotBlank String deviceId, //to remember whichever device captured this route pings
        @NotBlank UUID captureSessionId,
        @NotBlank Long sequenceNumber,//last += 1
        @NotBlank double lat,
        @NotBlank double lan
        ) {}
