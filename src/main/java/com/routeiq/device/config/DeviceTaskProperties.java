package com.routeiq.device.config;

import com.routeiq.device.constants.DeviceTaskConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DeviceTaskProperties {

    private final int postGeoPollIntervalSeconds;
    private final int serveAdPollIntervalSeconds;

    public DeviceTaskProperties(
            @Value("${device.tasks.post-geo.poll-interval-seconds}") int postGeoPollIntervalSeconds,
            @Value("${device.tasks.serve-ad.poll-interval-seconds}") int serveAdPollIntervalSeconds) {
        this.postGeoPollIntervalSeconds = requirePositive(DeviceTaskConstants.POST_GEO, postGeoPollIntervalSeconds);
        this.serveAdPollIntervalSeconds = requirePositive(DeviceTaskConstants.SERVE_AD, serveAdPollIntervalSeconds);
    }

    public int pollIntervalSeconds(String task) {
        return switch (task) {
            case DeviceTaskConstants.POST_GEO -> postGeoPollIntervalSeconds;
            case DeviceTaskConstants.SERVE_AD -> serveAdPollIntervalSeconds;
            case DeviceTaskConstants.MUTE -> 0;
            default -> throw new IllegalArgumentException("Unsupported device task: " + task);
        };
    }

    private int requirePositive(String task, int intervalSeconds) {
        if (intervalSeconds <= 0) {
            throw new IllegalArgumentException(
                    "Poll interval for " + task + " must be greater than zero"
            );
        }
        return intervalSeconds;
    }
}
