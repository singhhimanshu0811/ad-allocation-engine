package com.routeiq.device.model;

import java.time.Instant;

public record ImageRow(
        String imageUrl,
        Instant timestamp
) {}
