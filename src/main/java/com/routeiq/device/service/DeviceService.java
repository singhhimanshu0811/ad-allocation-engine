package com.routeiq.device.service;

import com.routeiq.device.config.DelayKalmanFilter;
import com.routeiq.device.entity.*;
import com.routeiq.device.model.*;
import com.routeiq.device.config.DeviceTaskProperties;
import com.routeiq.device.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.*;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class DeviceService {

    private final DeviceCredentialRepository deviceCredentialRepository;
    private final GeoLocationRepository geoLocationRepository;
    private final HeartbeatRepository heartbeatRepository;
    private final ImageRepository imageRepository;
    private final DeviceTaskRepository deviceTaskRepository;
    private final DeviceRouteRepository deviceRouteRepository;
    private final DeviceTaskProperties deviceTaskProperties;
    private final EntityManager entityManager;
    private final RouteStopsRepository routeStopsRepository;
    private final RouteSpatialRepository routeSpatialRepository;
    private final DeviceCurrentStateRepository deviceCurrentStateRepository;

    private final ModelMapper modelMapper;



    private final GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);


    private static final double PROCESS_NOISE = 0.1;
    private static final double MEASUREMENT_NOISE = 2.0;

    public DeviceService(DeviceCredentialRepository deviceCredentialRepository,
                         GeoLocationRepository geoLocationRepository,
                         HeartbeatRepository heartbeatRepository,
                         ImageRepository imageRepository,
                         DeviceTaskRepository deviceTaskRepository,
                         DeviceRouteRepository deviceRouteRepository,
                         DeviceTaskProperties deviceTaskProperties,
                         RouteStopsRepository routeStopsRepository,
                         RouteSpatialRepository routeSpatialRepository,
                         DeviceCurrentStateRepository deviceCurrentStateRepository,
                         EntityManager entityManager,
                         ModelMapper modelMapper) {
        this.deviceCredentialRepository = deviceCredentialRepository;
        this.geoLocationRepository = geoLocationRepository;
        this.heartbeatRepository = heartbeatRepository;
        this.imageRepository = imageRepository;
        this.deviceTaskRepository = deviceTaskRepository;
        this.deviceRouteRepository = deviceRouteRepository;
        this.routeStopsRepository = routeStopsRepository;
        this.routeSpatialRepository = routeSpatialRepository;
        this.deviceCurrentStateRepository = deviceCurrentStateRepository;
        this.deviceTaskProperties = deviceTaskProperties;
        this.entityManager = entityManager;
        this.modelMapper = modelMapper;
    }

    @Transactional
    public TaskResponse getDeviceTask(String deviceId) {
        return deviceTaskRepository.findById(deviceId)
                .map(task -> new TaskResponse(task.getTask(), task.getPollIntervalSeconds()))
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "No task configured for device: " + deviceId
                ));
    }

    @Transactional
    public TaskResponse saveDeviceTask(SaveTaskRequest request) {
        int pollIntervalSeconds = deviceTaskProperties.pollIntervalSeconds(request.task());
        DeviceTaskEntity task = deviceTaskRepository.findById(request.deviceId())
                .orElseGet(() -> new DeviceTaskEntity(
                        request.deviceId(),
                        pollIntervalSeconds,
                        request.task()
                ));
        task.setTask(request.task().toUpperCase());
        task.setPollIntervalSeconds(pollIntervalSeconds);
        DeviceTaskEntity savedTask = deviceTaskRepository.save(task);
        return new TaskResponse(savedTask.getTask(), savedTask.getPollIntervalSeconds());
    }

    @Transactional
    public RouteResponse saveRoute(SaveRouteRequest request) {

        List<GeoLocationEntity> geoLocationEntities = geoLocationRepository.findByCaptureSessionIdOrderByTimestampAsc(request.captureSessionId());

        Coordinate[] coords = geoLocationEntities.stream()
                .map(p -> new Coordinate(p.getLan(), p.getLat()))
                .toArray(Coordinate[]::new);

        RouteEntity route = new RouteEntity();
        route.setRouteName(request.routeName());
        route.setPath(gf.createLineString(coords));

        return new RouteResponse(route.getRouteId());
    }

    @Transactional
    public Boolean saveGeoLocations(SaveGeoLocationsPingRequest request) {

       try {
            GeoLocationEntity geoLocation = new GeoLocationEntity();
            geoLocation.setDeviceId(request.deviceId());
            geoLocation.setCaptureSessionId(request.captureSessionId());
            geoLocation.setLat(request.lat());
            geoLocation.setLan(request.lan());
            geoLocation.setSequenceNumber(request.sequenceNumber());

            geoLocation.setGeneratedAt(java.time.Instant.now());

            geoLocationRepository.save(geoLocation);

            return true;
        }

       catch (Exception e) {
           log.error("Error saving geo location for device {}: {}", request.deviceId(), e.getMessage());
            return false;
        }
    }

    public List<ImageRow> getImages(String deviceId, GeoLocation location) {
        return imageRepository.findByDeviceId(deviceId).stream()
                .map(image -> new ImageRow(image.getImageUrl(), image.getTimestamp()))
                .toList();
    }

    @Transactional
    public String heartbeat(HeartbeatRequest request) {
      //todo

//        DeviceCurrentStateEntity state = deviceCurrentStateRepository.findById(request.deviceId())
//                .orElseGet(() -> {
//                    DeviceCurrentStateEntity s = new DeviceCurrentStateEntity();
//                    s.setDeviceId(request.deviceId());
//                    s.setSmoothedDelay(0.0);
//                    s.setErrorCovariance(1.0); // initial uncertainty - need to play with it
//                    return s;
//                });
//
//        Point position = gf.createPoint(new Coordinate(request.lon(), request.lat()));
//        state.setCurrentRouteId(request.routeId());
//        state.setPosition(position);
//        state.setDistanceAlongRoute(request.distanceAlongRoute());
//        state.setSmoothedDelay(request.smoothedDelay());
//        state.setErrorCovariance(filter.getErrorCovariance());
//        state.setUpdatedAt(pingTime);
//        deviceCurrentStateRepository.save(state);

        return "ok";
    }

    @Transactional
    public void onPing(String deviceId, double lat, double lon, Instant pingTime){
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
            Instant normalizedScheduledTime = timeOfDay.atDate(today).toInstant(ZoneOffset.UTC);

            rawDelaySeconds = Duration.between(normalizedScheduledTime, pingTime).getSeconds();
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

         //step 7 : todo : append in historical log

    }
}
