package com.routeiq.device.service;

import com.routeiq.device.entity.*;
import com.routeiq.device.model.*;
import com.routeiq.device.config.DeviceTaskProperties;
import com.routeiq.device.constants.DeviceTaskConstants;
import com.routeiq.device.repository.DeviceCredentialRepository;
import com.routeiq.device.repository.DeviceTaskRepository;
import com.routeiq.device.repository.DeviceRouteRepository;
import com.routeiq.device.repository.GeoLocationRepository;
import com.routeiq.device.repository.HeartbeatRepository;
import com.routeiq.device.repository.ImageRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Null;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

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

    private final ModelMapper modelMapper;

    private final GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);

    public DeviceService(DeviceCredentialRepository deviceCredentialRepository,
                         GeoLocationRepository geoLocationRepository,
                         HeartbeatRepository heartbeatRepository,
                         ImageRepository imageRepository,
                         DeviceTaskRepository deviceTaskRepository,
                         DeviceRouteRepository deviceRouteRepository,
                         DeviceTaskProperties deviceTaskProperties,
                         EntityManager entityManager,
                         ModelMapper modelMapper) {
        this.deviceCredentialRepository = deviceCredentialRepository;
        this.geoLocationRepository = geoLocationRepository;
        this.heartbeatRepository = heartbeatRepository;
        this.imageRepository = imageRepository;
        this.deviceTaskRepository = deviceTaskRepository;
        this.deviceRouteRepository = deviceRouteRepository;
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

            geoLocation.setGeneratedAt(Timestamp.from(java.time.Instant.now()));

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
    public String heartbeat(String deviceId, Boolean heartbeat) {
        DeviceCredentialEntity device = entityManager.getReference(DeviceCredentialEntity.class, deviceId);
        heartbeatRepository.save(new HeartbeatEntity(device, heartbeat, java.time.Instant.now()));
        return "OKay";
    }
}
