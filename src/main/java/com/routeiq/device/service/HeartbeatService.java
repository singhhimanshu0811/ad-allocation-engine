package com.routeiq.device.service;
import com.routeiq.device.entity.*;
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
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class HeartbeatService {
    private final RouteRepository routeRepository;

    private final HeartbeatRepository heartbeatRepository;
    private final Duration retentionPeriod;

    private final RouteStopsRepository routeStopsRepository;
    private final RouteSpatialRepository routeSpatialRepository;
    private final DeviceCurrentStateRepository deviceCurrentStateRepository;

    private final GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);

    private static final double PROCESS_NOISE = 0.1;
    private static final double MEASUREMENT_NOISE = 2.0;

    public HeartbeatService(
            HeartbeatRepository heartbeatRepository,
            RouteStopsRepository routeStopsRepository,
            RouteSpatialRepository routeSpatialRepository,
            DeviceCurrentStateRepository deviceCurrentStateRepository,
            @Value("${heartbeat.cleanup.retention-days:7}") long retentionDays,
            RouteRepository routeRepository) {
        this.heartbeatRepository = heartbeatRepository;
        if (retentionDays <= 0) {
            throw new IllegalArgumentException("heartbeat.cleanup.retention-days must be greater than zero");
        }
        this.retentionPeriod = Duration.ofDays(retentionDays);
        this.deviceCurrentStateRepository = deviceCurrentStateRepository;
        this.routeSpatialRepository= routeSpatialRepository;
        this.routeStopsRepository = routeStopsRepository;
        this.routeRepository = routeRepository;
    }

//    @Scheduled(fixedDelayString = "${heartbeat.cleanup.fixed-delay-ms:43200000}")
//    @Transactional
//    public void deleteOldHeartbeats() {
//        heartbeatRepository.deleteOlderThan(Instant.now().minus(retentionPeriod));
//    }

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
        Optional<RouteEntity> optionalAssignment = routeRepository
                .findNearestRoute(lat, lon);

        if(optionalAssignment.isEmpty()){
            //nothing is there for this device, so we can just return
            log.warn("No active route assignment found for device: {}", deviceId);
            return;
        }


//each device id will have only one route mapped to it - so we can safely extract route id from here
        Long routeId = optionalAssignment.get().getRouteId();

        //step2 = find the distance along the route for the given lat/lon for given stops
        double distanceAlongRoute = routeSpatialRepository.locateDistanceAlongRoute(routeId, lat, lon);


        DeviceCurrentStateEntity state = deviceCurrentStateRepository.findById(deviceId)
                .orElseGet(() -> {
                    DeviceCurrentStateEntity s = new DeviceCurrentStateEntity();
                    s.setDeviceId(deviceId);
                    return s;
                });

        //step 6 : persist current state of device
        Point position = gf.createPoint(new Coordinate(lon, lat));
        state.setCurrentRouteId(optionalAssignment.get());
        state.setPosition(position);
        state.setDistanceAlongRoute(distanceAlongRoute);
        state.setUpdatedAt(pingTime);
        deviceCurrentStateRepository.save(state);

        //step 7 : store in heartbeat entity
        HeartbeatEntity heartbeatEntity = new HeartbeatEntity();
        heartbeatEntity.setDeviceId(deviceId);
        heartbeatEntity.setDistanceAlongRoute(distanceAlongRoute);
        heartbeatEntity.setReceivedAt(pingTime);
        heartbeatEntity.setHeartbeat(true);
        heartbeatEntity.setRouteId(routeId);
        heartbeatEntity.setSpeed(request.speed());//converting km/h in m/s
        heartbeatRepository.save(heartbeatEntity);

    }
}
