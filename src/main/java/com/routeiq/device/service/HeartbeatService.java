package com.routeiq.device.service;

import com.routeiq.device.config.DelayKalmanFilter;
import com.routeiq.device.entity.DeviceCurrentStateEntity;
import com.routeiq.device.entity.DeviceRouteEntity;
import com.routeiq.device.entity.HeartbeatEntity;
import com.routeiq.device.entity.RouteStopEntity;
import com.routeiq.device.model.HeartbeatRequest;
import com.routeiq.device.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class HeartbeatService {

    private final HeartbeatRepository heartbeatRepository;
    private final Duration retentionPeriod;

    private final DeviceRouteRepository deviceRouteRepository;
    private final RouteStopsRepository routeStopsRepository;
    private final RouteSpatialRepository routeSpatialRepository;
    private final DeviceCurrentStateRepository deviceCurrentStateRepository;

    private final GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);

    private static final double PROCESS_NOISE = 0.1;
    private static final double MEASUREMENT_NOISE = 2.0;

    public HeartbeatService(
            HeartbeatRepository heartbeatRepository,
            DeviceRouteRepository deviceRouteRepository,
            RouteStopsRepository routeStopsRepository,
            RouteSpatialRepository routeSpatialRepository,
            DeviceCurrentStateRepository deviceCurrentStateRepository,
            @Value("${heartbeat.cleanup.retention-days:7}") long retentionDays) {
        this.heartbeatRepository = heartbeatRepository;
        if (retentionDays <= 0) {
            throw new IllegalArgumentException("heartbeat.cleanup.retention-days must be greater than zero");
        }
        this.retentionPeriod = Duration.ofDays(retentionDays);
        this.deviceCurrentStateRepository = deviceCurrentStateRepository;
        this.deviceRouteRepository = deviceRouteRepository;
        this.routeSpatialRepository= routeSpatialRepository;
        this.routeStopsRepository = routeStopsRepository;
    }

    @Scheduled(fixedDelayString = "${heartbeat.cleanup.fixed-delay-ms:43200000}")
    @Transactional
    public void deleteOldHeartbeats() {
        heartbeatRepository.deleteOlderThan(Instant.now().minus(retentionPeriod));
    }

    @Transactional
    public void recordHeartBeat(HeartbeatRequest request){
        String deviceId = request.deviceId();
        double lat = request.lat(), lon = request.lon();

        Instant pingTime = request.pingTime();
        boolean active = request.heartbeat();

        if(!active){
            //todo : what does active mean here?
        }
        //serve the device
        //step1 = find which device is calling for content
        Optional<DeviceRouteEntity> optionalAssignment = deviceRouteRepository
                .findByDeviceIdAndActiveTrue(deviceId);

        if(optionalAssignment.isEmpty()){
            //nothing is there for this device, so we can just return
            log.warn("No active route assignment found for device: {}", deviceId);
            return;
        }


        Long routeId = optionalAssignment.get().getRoute().getRouteId();

        //step2 = find the distance along the route for the given lat/lon for given stops
        double distanceAlongRoute = routeSpatialRepository.locateDistanceAlongRoute(routeId, lat, lon);

        //step3 : find bracketing stops
        List<RouteStopEntity> stops = routeStopsRepository.findByRouteIdOrderBySequenceNumberAsc(routeId);


        RouteStopEntity before = null, after = null;
        for (RouteStopEntity stop : stops) {
            if (stop.getDistanceAlongRoute() <= distanceAlongRoute) before = stop; //finding the last previous stop that is less than or equal to the distance along the route
            if (stop.getDistanceAlongRoute() >= distanceAlongRoute && after == null) after = stop;//finding the next stop that is greater than or equal to the distance along the route
        }

        //step4: we know for each route - what is the delay between stop - so then we calculate what is the expected time and if there is some delay
        double rawDelaySeconds = 0.0;
        if (before != null && after != null && !before.equals(after)) {
            double fraction = (distanceAlongRoute - before.getDistanceAlongRoute())
                    / (after.getDistanceAlongRoute() - before.getDistanceAlongRoute());

            Instant beforeTime = before.getScheduledTime();
            Instant afterTime = after.getScheduledTime();
            long segmentSeconds = Duration.between(beforeTime, afterTime).getSeconds();
            long offsetSeconds = Math.round(fraction * segmentSeconds);

            Instant interpolatedScheduledTime = beforeTime.plusSeconds(offsetSeconds);

            // re-anchor to today's date, keeping only the time-of-day component
            LocalTime timeOfDay = interpolatedScheduledTime.atZone(ZoneOffset.UTC).toLocalTime();
            LocalDate today = pingTime.atZone(ZoneOffset.UTC).toLocalDate();
            Instant normalizedInterpolatedScheduledTime = timeOfDay.atDate(today).toInstant(ZoneOffset.UTC);

            rawDelaySeconds = Duration.between(normalizedInterpolatedScheduledTime, pingTime).getSeconds();
        }

        // else: before end/start of stop list — no valid bracket, skip delay calc for this ping

        DeviceCurrentStateEntity state = deviceCurrentStateRepository.findById(deviceId)
                .orElseGet(() -> {
                    DeviceCurrentStateEntity s = new DeviceCurrentStateEntity();
                    s.setDeviceId(deviceId);
                    s.setSmoothedDelay(0.0);
                    s.setErrorCovariance(1.0); // initial uncertainty - need to play with it
                    return s;
                });

        DelayKalmanFilter filter = new DelayKalmanFilter(
                state.getSmoothedDelay(), state.getErrorCovariance(),
                PROCESS_NOISE, MEASUREMENT_NOISE);
        double smoothedDelay = filter.update(rawDelaySeconds);

        //step 6 : persist current state of device
        Point position = gf.createPoint(new Coordinate(lon, lat));
        state.setCurrentRouteId(routeId);
        state.setPosition(position);
        state.setDistanceAlongRoute(distanceAlongRoute);
        state.setSmoothedDelay(smoothedDelay);
        state.setErrorCovariance(filter.getErrorCovariance());
        state.setUpdatedAt(pingTime);
        deviceCurrentStateRepository.save(state);

        //step 7 : store in heartbeat entity
        HeartbeatEntity heartbeatEntity = new HeartbeatEntity();
        heartbeatEntity.setDeviceId(deviceId);
        heartbeatEntity.setDistanceAlongRoute(distanceAlongRoute);
        heartbeatEntity.setReceivedAt(pingTime);
        heartbeatEntity.setHeartbeat(true);
        heartbeatEntity.setRouteId(routeId);
        heartbeatRepository.save(heartbeatEntity);

    }
}
