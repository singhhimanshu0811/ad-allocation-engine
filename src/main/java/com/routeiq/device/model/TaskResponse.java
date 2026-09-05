package com.routeiq.device.model;

public record TaskResponse(
        String task,
        int pollIntervalSeconds
) {
}
