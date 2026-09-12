package com.routeiq.device.controller;

import com.routeiq.device.model.*;
import com.routeiq.device.service.DeviceService;
import com.routeiq.device.service.HeartbeatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.List;

@RestController
@RequestMapping("/adservice/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    private final HeartbeatService heartbeatService;

    public DeviceController(DeviceService deviceService, HeartbeatService heartbeatService) {

        this.deviceService = deviceService;
        this.heartbeatService = heartbeatService;
    }

    @GetMapping("/task")
    public TaskResponse getDeviceTask(@RequestParam @NotBlank String deviceId) {
        return deviceService.getDeviceTask(deviceId);
    }

    @PutMapping(
            value = "/task",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public TaskResponse saveDeviceTask(@Valid @RequestBody SaveTaskRequest request) {
        return deviceService.saveDeviceTask(request);
    }

    @PostMapping(
            value = "/routes",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public RouteResponse saveRoute(@Valid @RequestBody SaveRouteRequest request) {
        return deviceService.saveRoute(request);
    }

    @PostMapping(
            value = "/geo",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Boolean> saveGeoLocations(@Valid @RequestBody SaveGeoLocationsPingRequest request) {
        boolean result = deviceService.saveGeoLocations(request);
        return result ? ResponseEntity.ok(true) : ResponseEntity.badRequest().body(false);
    }

    @GetMapping(
            value = "/content",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public List<ImageRow> getContentForDevice(@Valid @RequestBody GetImagesRequest request) {
        return deviceService.getContent(request.deviceId(), request.location());
    }

    @PostMapping(
            value = "/heartbeat",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public void heartbeat(@Valid @RequestBody HeartbeatRequest request) {
        heartbeatService.recordHeartBeat(request);
    }

    //todo : this controller should only be called by admins - manually - maybe something like SR authentication required

    @PostMapping(
            value = "/periodic-allocation",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public void runPeriodicAllocation(@RequestBody AllocationTimeIntervalModel model){
        ZoneId indiaZone = ZoneId.of("Asia/Kolkata");

        LocalDateTime localDateTime = LocalDateTime.of(model.startDate(), model.startTime());
        Instant startInstant = localDateTime.atZone(indiaZone).toInstant();

        localDateTime = LocalDateTime.of(model.endDate(), model.endTime());
        Instant endInstant = localDateTime.atZone(indiaZone).toInstant();

        deviceService.writeContentMetadatForWindow(startInstant, endInstant, false);
    }

    @PostMapping(
            value = "/periodic-allocation-demo",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public void runPeriodicAllocationDemo(LocalDate localDate){
        Instant earliestInstant = localDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant latestInstant = localDate.atTime(23, 59, 59, 999_999_999).toInstant(ZoneOffset.UTC);

        deviceService.writeContentMetadatForWindow(earliestInstant, latestInstant, true);
    }
}
