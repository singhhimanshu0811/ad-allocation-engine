package com.routeiq.device.service;

import com.routeiq.device.model.GeoLocation;
import com.routeiq.device.model.ImageRow;
import com.routeiq.device.model.SaveRouteRequest;
import com.routeiq.device.model.TaskResponse;
import com.routeiq.device.model.RouteResponse;
import com.routeiq.device.model.SaveTaskRequest;
import com.routeiq.device.entity.GeoLocationEntity;
import com.routeiq.device.entity.HeartbeatEntity;
import com.routeiq.device.entity.DeviceRouteEntity;
import com.routeiq.device.entity.DeviceTaskEntity;
import com.routeiq.device.entity.DeviceCredentialEntity;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
public class DeviceService {

    private final DeviceCredentialRepository deviceCredentialRepository;
    private final GeoLocationRepository geoLocationRepository;
    private final HeartbeatRepository heartbeatRepository;
    private final ImageRepository imageRepository;
    private final DeviceTaskRepository deviceTaskRepository;
    private final DeviceRouteRepository deviceRouteRepository;
    private final DeviceTaskProperties deviceTaskProperties;
    private final EntityManager entityManager;

    public DeviceService(DeviceCredentialRepository deviceCredentialRepository,
                         GeoLocationRepository geoLocationRepository,
                         HeartbeatRepository heartbeatRepository,
                         ImageRepository imageRepository,
                         DeviceTaskRepository deviceTaskRepository,
                         DeviceRouteRepository deviceRouteRepository,
                         DeviceTaskProperties deviceTaskProperties,
                         EntityManager entityManager) {
        this.deviceCredentialRepository = deviceCredentialRepository;
        this.geoLocationRepository = geoLocationRepository;
        this.heartbeatRepository = heartbeatRepository;
        this.imageRepository = imageRepository;
        this.deviceTaskRepository = deviceTaskRepository;
        this.deviceRouteRepository = deviceRouteRepository;
        this.deviceTaskProperties = deviceTaskProperties;
        this.entityManager = entityManager;
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
        DeviceRouteEntity existingRoute = deviceRouteRepository
                .findByDeviceIdAndFromLocationAndToLocation(
                        request.deviceId(),
                        request.fromLocation(),
                        request.toLocation()
                )
                .orElse(null);

        if (existingRoute != null && existingRoute.isActive()) {
            return new RouteResponse(existingRoute.getId());
        }

        deviceRouteRepository.deactivateActiveRoute(request.deviceId());

        Long routeId;
        if (existingRoute != null) {
            existingRoute.setActive(true);
            routeId = deviceRouteRepository.save(existingRoute).getId();
        } else {
            routeId = deviceRouteRepository.save(new DeviceRouteEntity(
                    request.deviceId(),
                    request.fromLocation(),
                    request.toLocation(),
                    true
            )).getId();
        }

        String taskName = DeviceTaskConstants.POST_GEO;
        DeviceTaskEntity task = deviceTaskRepository.findById(request.deviceId())
                .orElseGet(() -> new DeviceTaskEntity(
                        request.deviceId(),
                        deviceTaskProperties.pollIntervalSeconds(taskName),
                        taskName
                ));
        task.setTask(DeviceTaskConstants.POST_GEO);
        task.setPollIntervalSeconds(deviceTaskProperties.pollIntervalSeconds(taskName));
        deviceTaskRepository.save(task);

        return new RouteResponse(routeId);
    }

    @Transactional
    public String saveGeoLocations(String deviceId, Long routeId, List<GeoLocation> locations) {
        DeviceRouteEntity activeRoute = deviceRouteRepository.findByDeviceIdAndActiveTrue(deviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "No active route configured for device: " + deviceId
                ));

        if (!Objects.equals(activeRoute.getId(), routeId)) {
            throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED,
                    "Active route configured did not match request scope route for device: " + deviceId);
        }

        geoLocationRepository.saveAll(locations.stream()
                .map(location -> new GeoLocationEntity(
                        activeRoute,
                        location.lat(),
                        location.lang(),
                        location.speed(),
                        location.heading(),
                        location.generatedAt()
                ))
                .toList());

        return "saved okay";
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
