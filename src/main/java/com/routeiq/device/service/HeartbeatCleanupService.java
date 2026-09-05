package com.routeiq.device.service;

import com.routeiq.device.repository.HeartbeatRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class HeartbeatCleanupService {

    private final HeartbeatRepository heartbeatRepository;
    private final Duration retentionPeriod;

    public HeartbeatCleanupService(
            HeartbeatRepository heartbeatRepository,
            @Value("${heartbeat.cleanup.retention-days:7}") long retentionDays) {
        this.heartbeatRepository = heartbeatRepository;
        if (retentionDays <= 0) {
            throw new IllegalArgumentException("heartbeat.cleanup.retention-days must be greater than zero");
        }
        this.retentionPeriod = Duration.ofDays(retentionDays);
    }

    @Scheduled(fixedDelayString = "${heartbeat.cleanup.fixed-delay-ms:43200000}")
    @Transactional
    public void deleteOldHeartbeats() {
        heartbeatRepository.deleteOlderThan(Instant.now().minus(retentionPeriod));
    }
}
