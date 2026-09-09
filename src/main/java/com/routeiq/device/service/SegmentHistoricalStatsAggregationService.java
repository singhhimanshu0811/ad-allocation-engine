package com.routeiq.device.service;

import com.routeiq.device.entity.HeartbeatEntity;
import com.routeiq.device.entity.RouteEntity;
import com.routeiq.device.entity.RouteStopEntity;
import com.routeiq.device.entity.SegmentHistoricalStatsEntity;
import com.routeiq.device.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SegmentHistoricalStatsAggregationService {

    @Autowired
    HeartbeatRepository heartBeatRepository;
    @Autowired
    RouteStopsRepository routeStopRepository;
    @Autowired
    SegmentHistoricalStatsRepository statsRepository;
    @Autowired
    RouteRepository routeRepository;

    @Transactional
    public void aggregateHistoricalStats(LocalDate targetDate) {

        Instant dayStart = targetDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd = dayStart.plus(1, ChronoUnit.DAYS);

        List<RouteEntity> allRoutes = routeRepository.findAll();

        for (RouteEntity route : allRoutes) {
            List<RouteStopEntity> stops = routeStopRepository
                    .findByRouteIdOrderByStopSequenceNumberAsc(route.getRouteId());

            if (stops.size() < 2) continue; // need at least 2 stops to form a segment

            List<HeartbeatEntity> pings = heartBeatRepository
                    .findByRouteIdAndReceivedAtBetweenOrderByDeviceIdAscReceivedAtAsc(route.getRouteId(), dayStart, dayEnd);

            // group pings by device — each device's trip through the route is independent evidence
            Map<String, List<HeartbeatEntity>> pingsByDevice = pings.stream()
                    .collect(Collectors.groupingBy(HeartbeatEntity::getDeviceId));

            // accumulate raw segment durations before computing mean/variance
            Map<String, List<Double>> segmentDurations = new HashMap<>(); // key: "startDist|endDist|hour|dow"

            for (List<HeartbeatEntity> devicePings : pingsByDevice.values()) {
                for (int i = 0; i < stops.size() - 1; i++) {
                    RouteStopEntity segmentStart = stops.get(i);
                    RouteStopEntity segmentEnd = stops.get(i + 1);

                    Instant crossingTimeStart = findCrossingTime(devicePings, segmentStart.getDistanceAlongRoute());
                    Instant crossingTimeEnd = findCrossingTime(devicePings, segmentEnd.getDistanceAlongRoute());

                    if (crossingTimeStart == null || crossingTimeEnd == null) continue; // device didn't cover this segment today

                    double durationSeconds = Duration.between(crossingTimeStart, crossingTimeEnd).getSeconds();
                    if (durationSeconds <= 0) continue; // skip bad/out-of-order data

                    int hourOfDay = crossingTimeStart.atZone(ZoneOffset.UTC).getHour();
                    int dayOfWeek = crossingTimeStart.atZone(ZoneOffset.UTC).getDayOfWeek().getValue();

                    String key = segmentStart.getDistanceAlongRoute() + "|" + segmentEnd.getDistanceAlongRoute()
                            + "|" + hourOfDay + "|" + dayOfWeek;

                    segmentDurations.computeIfAbsent(key, k -> new ArrayList<>()).add(durationSeconds);
                }
            }

            // compute mean/variance per segment/hour/day bucket, merge with existing stats
            for (Map.Entry<String, List<Double>> entry : segmentDurations.entrySet()) {
                String[] parts = entry.getKey().split("\\|");
                double startDist = Double.parseDouble(parts[0]);
                double endDist = Double.parseDouble(parts[1]);
                int hourOfDay = Integer.parseInt(parts[2]);
                int dayOfWeek = Integer.parseInt(parts[3]);

                List<Double> durations = entry.getValue();
                double mean = durations.stream().mapToDouble(d -> d).average().orElse(0.0);
                double variance = durations.stream()
                        .mapToDouble(d -> Math.pow(d - mean, 2))
                        .average().orElse(0.0);

                SegmentHistoricalStatsEntity stats = statsRepository
                        .findByRouteIdAndSegmentStartDistanceAndSegmentEndDistanceAndHourOfDayAndDayOfWeek(
                                route.getRouteId(), startDist, endDist, hourOfDay, dayOfWeek)
                        .orElseGet(SegmentHistoricalStatsEntity::new);

                if (stats.getSampleCount() != null && stats.getSampleCount() > 0) {
                    // merge new day's samples with existing running stats (weighted combine)
                    int oldCount = stats.getSampleCount();
                    int newCount = durations.size();
                    int totalCount = oldCount + newCount;

                    double combinedMean = (stats.getMeanTime() * oldCount + mean * newCount) / totalCount;
                    // simplified variance merge (approximate, good enough for this use case)
                    double combinedVariance = (stats.getVariance() * oldCount + variance * newCount) / totalCount;

                    stats.setMeanTime(combinedMean);
                    stats.setVariance(combinedVariance);
                    stats.setSampleCount(totalCount);
                } else {
                    stats.setRouteId(route.getRouteId());
                    stats.setSegmentStartDistance(startDist);
                    stats.setSegmentEndDistance(endDist);
                    stats.setHourOfDay(hourOfDay);
                    stats.setDayOfWeek(dayOfWeek);
                    stats.setMeanTime(mean);
                    stats.setVariance(variance);
                    stats.setSampleCount(durations.size());
                }

                statsRepository.save(stats);
            }
        }
    }

    /**
     * Finds the interpolated timestamp at which a device's ping trail crosses
     * a given distance-along-route value, using the two bracketing pings.
     */
    private Instant findCrossingTime(List<HeartbeatEntity> devicePingsSortedByTime, double targetDistance) {
        //given these pings - at what time did the device cross this target distance point on the route
        for (int i = 0; i < devicePingsSortedByTime.size() - 1; i++) {
            HeartbeatEntity before = devicePingsSortedByTime.get(i);
            HeartbeatEntity after = devicePingsSortedByTime.get(i + 1);

            if (before.getDistanceAlongRoute() <= targetDistance && after.getDistanceAlongRoute() >= targetDistance//target distance is a point
                    // between two pings - which might not be stops
                    && after.getDistanceAlongRoute() != before.getDistanceAlongRoute()) {

                double fraction = (targetDistance - before.getDistanceAlongRoute())
                        / (after.getDistanceAlongRoute() - before.getDistanceAlongRoute());

                long segmentSeconds = Duration.between(before.getReceivedAt(), after.getReceivedAt()).getSeconds();
                long offsetSeconds = Math.round(fraction * segmentSeconds);

                return before.getReceivedAt().plusSeconds(offsetSeconds);
            }
        }
        return null; // target distance never bracketed by this device's pings today
    }

    
}
