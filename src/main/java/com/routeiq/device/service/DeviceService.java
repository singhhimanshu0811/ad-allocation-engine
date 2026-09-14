package com.routeiq.device.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.routeiq.device.config.AllocationCostCalculator;
import com.routeiq.device.entity.*;
import com.routeiq.device.model.*;
import com.routeiq.device.config.DeviceTaskProperties;
import com.routeiq.device.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DeviceService {
    @Autowired
    private RouteRepository routeRepository;

    private final DeviceCredentialRepository deviceCredentialRepository;
    private final GeoLocationRepository geoLocationRepository;
    private final HeartbeatRepository heartbeatRepository;
    private final ImageRepository imageRepository;
    private final DeviceTaskRepository deviceTaskRepository;
    private final DeviceTaskProperties deviceTaskProperties;
    private final EntityManager entityManager;
    private final RouteStopsRepository routeStopsRepository;
    private final RouteSpatialRepository routeSpatialRepository;
    private final DeviceCurrentStateRepository deviceCurrentStateRepository;
    private final RouteCampaignCandidateRepository routeCampaignCandidateRepository;
    private final CampaignRepository campaignRepository;
    private final PeriodicAllocationRepository periodicAllocationRepository;
    private final DemoPeriodicAllocationRepository demoPeriodicAllocationRepository;

    @Autowired
    private  AllocationCostCalculator allocationCostCalculator;


    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())//needed so that time can be saved in jsonb module
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);


    public record EligiblePair(String deviceId, String campaignId, double cost, Long timeToReach, Instant predictedEntryTime) {}
    public record AllocationResult(String deviceId, String campaignId, int allocatedPlays) {}



    private final GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);

    public DeviceService(DeviceCredentialRepository deviceCredentialRepository,
                         GeoLocationRepository geoLocationRepository,
                         HeartbeatRepository heartbeatRepository,
                         ImageRepository imageRepository,
                         DeviceTaskRepository deviceTaskRepository,
                         DeviceTaskProperties deviceTaskProperties,
                         RouteStopsRepository routeStopsRepository,
                         RouteSpatialRepository routeSpatialRepository,
                         DeviceCurrentStateRepository deviceCurrentStateRepository,
                         RouteCampaignCandidateRepository routeCampaignCandidateRepository,
                         CampaignRepository campaignRepository,
                         PeriodicAllocationRepository periodicAllocationRepository,
                         DemoPeriodicAllocationRepository demoPeriodicAllocationRepository,
                         EntityManager entityManager) {
        this.deviceCredentialRepository = deviceCredentialRepository;
        this.geoLocationRepository = geoLocationRepository;
        this.heartbeatRepository = heartbeatRepository;
        this.imageRepository = imageRepository;
        this.deviceTaskRepository = deviceTaskRepository;
        this.routeStopsRepository = routeStopsRepository;
        this.routeSpatialRepository = routeSpatialRepository;
        this.deviceCurrentStateRepository = deviceCurrentStateRepository;
        this.routeCampaignCandidateRepository = routeCampaignCandidateRepository;
        this.campaignRepository = campaignRepository;
        this.periodicAllocationRepository = periodicAllocationRepository;
        this.demoPeriodicAllocationRepository = demoPeriodicAllocationRepository;
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

        List<GeoLocationEntity> geoLocationEntities = geoLocationRepository.findByCaptureSessionIdOrderByGeneratedAtAsc(request.captureSessionId());

        Coordinate[] coords = geoLocationEntities.stream()
                .map(p -> {
                    String strPositions = p.getPositions();
                    List<GeoLocation> positions = null;
                    try {
                        positions = (strPositions == null || strPositions.isBlank())
                                ? List.of()
                                : mapper.readValue(strPositions, new TypeReference<List<GeoLocation>>() {});
                    } catch (JsonProcessingException e) {
                        log.error("Unable to create route for request {} due to error parsing positions: {}", request, e.getMessage());
                        positions = List.of();
                    }
                    return positions.stream().map(pos -> new Coordinate(pos.lang(), pos.lat())).toArray(Coordinate[]::new);
                })
                .flatMap(java.util.Arrays::stream)
                .toArray(Coordinate[]::new);


        RouteEntity route = new RouteEntity();
        route.setRouteName(request.routeName());
        route.setPath(gf.createLineString(coords));

        route = routeRepository.save(route);

        return new RouteResponse(route.getRouteId());
    }

    @Transactional
    public Boolean saveGeoLocations(SaveGeoLocationsPingRequest request) {

       try {
            GeoLocationEntity geoLocation = new GeoLocationEntity();
            geoLocation.setDeviceId(request.deviceId());
            geoLocation.setCaptureSessionId(request.captureSessionId());
            geoLocation.setPositions(mapper.writeValueAsString(request.locations()));
            geoLocation.setSequenceNumber(request.sequenceNumber());

            geoLocation.setGeneratedAt(Instant.now());

            geoLocationRepository.save(geoLocation);

            return true;
        }

       catch (Exception e) {
           log.error("Error saving geo location for device {}: {}", request.deviceId(), e.getMessage());
            return false;
        }
    }

    public List<ImageRow> getContent(String deviceId, GeoLocation location) {

        //step 1 : find active route matches

        return imageRepository.findByDeviceId(deviceId).stream()
                .map(image -> new ImageRow(image.getImageUrl(), image.getTimestamp()))
                .toList();
    }




    @Transactional
    public void writeContentMetadatForWindow(Instant startWindow, Instant endWindow, Boolean demoModeForFullDay) {

        // 1. Active campaigns for this window
        List<Campaign> campaigns = campaignRepository.findAllCampaignsWindow(startWindow, endWindow);
//if no campaigns return
        if (campaigns.isEmpty()) {
            return;
        }

        Map<String, Campaign> campaignMap = campaigns.stream().collect(Collectors.toMap(Campaign::getId, Function.identity()));

        // 2. Devices that are currently active
        List<String> deviceIds = deviceCredentialRepository.findAllActiveDevices().stream().map(DeviceCredentialEntity::getDeviceId).toList();

        if (deviceIds.isEmpty()) {
            return;
        }

        //* 3. Get recent heartbeats for ALL devices
        List<HeartbeatEntity> heartbeats = heartbeatRepository.findRecentHeartbeats(deviceIds);

        Map<String, List<HeartbeatEntity>> heartbeatsByDevice = heartbeats.stream().
                collect(Collectors.groupingBy(HeartbeatEntity::getDeviceId));

        //get current state of each device and store it in map
        List<DeviceCurrentStateEntity>deviceCurrentStateEntityList = deviceCurrentStateRepository.findByDeviceIdIn(deviceIds);
//see how map strategy is different from heartbeats and deviceCurrentStateEntityList
        Map<String, DeviceCurrentStateEntity> deviceCurrentStateEntityMap =
                deviceCurrentStateEntityList.stream().collect(Collectors.toMap(
                        h -> h.getDeviceId(),
                        h -> h
                ));

       //for each device figure out route by 5 previous route pings and then mode
        Map<String, Long> deviceToRoute = new HashMap<>();

        Map<String, Double> averageSpeed = new HashMap<>();

        for (String deviceId : deviceIds) {

            List<HeartbeatEntity> deviceHeartbeats = heartbeatsByDevice.getOrDefault(deviceId, Collections.emptyList());

            deviceHeartbeats.stream()
                    .limit(5)
                    .map(HeartbeatEntity::getRouteId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.groupingBy(
                            Function.identity(),
                            Collectors.counting()
                    ))
                    .entrySet()
                    .stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey).ifPresent(routeId -> deviceToRoute.put(deviceId, routeId));

            Double averageSpeedWithinLast20Pings = deviceHeartbeats.stream()
                    .limit(20)
                    .map(HeartbeatEntity::getSpeed)
                    .filter(Objects::nonNull)
                    .collect(Collectors.averagingDouble(Double::doubleValue));

            averageSpeed.putIfAbsent(deviceId, averageSpeedWithinLast20Pings);

        }

        if (deviceToRoute.isEmpty()) {
            return;
        }

        List<Long> routeIds = new ArrayList<>(deviceToRoute.values());

        //Get all route IDs created for each campaign - this is written during creation
        List<RouteCampaignCandidateEntity> candidates = routeCampaignCandidateRepository.findActiveRouteMatchesForRouteIds(routeIds);

       //index route id to all campaigns for fatser fetch via route id
        Map<Long, List<RouteCampaignCandidateEntity>> candidatesByRoute = candidates.stream()
                    .collect(Collectors.groupingBy(c -> c.getRoute().getRouteId()));


        //build elgiblie pairs (all possible pairs from above maps for allocation)

        List<EligiblePair> eligiblePairs = new ArrayList<>();

        for (String deviceId : deviceIds) {
            //todo : if no ping for last some time, dont allocate
            //todo : if speed is not calculable, dont allocate - within this time there should be some speed - division by 0 should not happen

            Long routeId = deviceToRoute.get(deviceId);

            if (routeId == null) {
                continue;
            }

            List<RouteCampaignCandidateEntity> routeCandidates = candidatesByRoute.getOrDefault(routeId, Collections.emptyList());

            DeviceCurrentStateEntity deviceCurrentState = deviceCurrentStateEntityMap.get(deviceId);

            //for each device - does this pair of route-candidtae work?
            for (RouteCampaignCandidateEntity candidate : routeCandidates) {

                double remainingDistance = candidate.getEntryMarker() - deviceCurrentState.getDistanceAlongRoute();

                Double deviceAverageSpeed = averageSpeed.get(deviceId);

                Long timeToReach = remainingDistance <= 0 ? 0 : Objects.isNull(deviceAverageSpeed) ? Long.MAX_VALUE : Math.round(remainingDistance / averageSpeed.get(deviceId));

                //todo : once we constraint users based on radius, add another check if this device has passed exit marker for this campaign.
                // if it has, continue, dont add this as eligible pair even after intersection

                Campaign campaign = campaignMap.get(candidate.getCampaign().getId());

                if (campaign == null || campaign.getImpressions() <= 0) {
                    continue;
                }

               // Cost of assigning this campaign to this device.
                double distanceCost = allocationCostCalculator.computeDistanceCost(candidate, campaign);

                double urgencyCost = allocationCostCalculator.computeUrgencyFactor(campaign);

                double cost = distanceCost + urgencyCost;

                eligiblePairs.add(new EligiblePair(deviceId, campaign.getId(), cost, timeToReach, startWindow));

            }
        }

        if (eligiblePairs.isEmpty()) {
            return;
        }

        //allocate for this hour
//        System.out.println(eligiblePairs);
        Long duration = Math.abs(Duration.between(startWindow, endWindow).getSeconds());
        List<AllocationResult> allocations = allocationCostCalculator.allocateMinCostFlow(eligiblePairs, campaigns, duration);

        /*
         * 10. Persist allocations.
         */
        ZoneId indiaZone = ZoneId.of("Asia/Kolkata");
        List<PeriodicAllocationEntity> allocationEntities = new ArrayList<>();

        for (AllocationResult result : allocations) {

            if (result.allocatedPlays() <= 0) {
                continue;
            }

            PeriodicAllocationEntity allocation = new PeriodicAllocationEntity();

            allocation.setDeviceId(result.deviceId());
            allocation.setCampaignId(result.campaignId());
            allocation.setStartWindow(startWindow);
            allocation.setEndWindow(endWindow);
            allocation.setAllocatedPlays(result.allocatedPlays());
            allocation.setDateOfAllocation(startWindow.atZone(indiaZone).toLocalDate());
            allocation.setLocalStartTime(startWindow.atZone(indiaZone).toLocalTime());
            allocation.setLocalEndTime(endWindow.atZone(indiaZone).toLocalTime());
            allocationEntities.add(allocation);
        }

        if (demoModeForFullDay) {
            demoPeriodicAllocationRepository.saveAll(allocationEntities);
        } else {
            periodicAllocationRepository.saveAll(allocationEntities);
        }

        /*
         * 11. Reduce remaining campaign demand.
         */
        Map<String, Integer> allocatedByCampaign =
                allocationEntities.stream()
                        .collect(Collectors.groupingBy(PeriodicAllocationEntity::getCampaignId,
                                Collectors.summingInt(PeriodicAllocationEntity::getAllocatedPlays)));

        for (Campaign campaign : campaigns) {

            int allocated = allocatedByCampaign.getOrDefault(campaign.getId(), 0);

            if (allocated > 0) {
                campaign.setImpressions(campaign.getImpressions() - allocated);

                campaignRepository.save(campaign);
            }
        }
    }



}
