package com.routeiq.device.controller;

import com.routeiq.device.model.GetImagesRequest;
import com.routeiq.device.model.HeartbeatRequest;
import com.routeiq.device.model.ImageRow;
import com.routeiq.device.model.SaveGeoLocationsPingRequest;
import com.routeiq.device.model.SaveRouteRequest;
import com.routeiq.device.model.TaskResponse;
import com.routeiq.device.model.RouteResponse;
import com.routeiq.device.model.SaveTaskRequest;
import com.routeiq.device.service.DeviceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/adservice/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
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
            value = "/saveGeoLocations",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public Boolean saveGeoLocations(@Valid @RequestBody SaveGeoLocationsPingRequest request) {
        return deviceService.saveGeoLocations(request);
    }

    @PostMapping(
            value = "/images",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public List<ImageRow> getImages(@Valid @RequestBody GetImagesRequest request) {
        return deviceService.getImages(request.deviceId(), request.location());
    }

    @PostMapping(
            value = "/heartbeat",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public String heartbeat(@Valid @RequestBody HeartbeatRequest request) {
        return deviceService.heartbeat(
                request
        );
    }
}
