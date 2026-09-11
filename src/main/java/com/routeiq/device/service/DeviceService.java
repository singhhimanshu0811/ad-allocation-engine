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
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private final DeviceRouteRepository deviceRouteRepository;
    private final DeviceTaskProperties deviceTaskProperties;
    private final EntityManager entityManager;
    private final RouteStopsRepository routeStopsRepository;
    private final RouteSpatialRepository routeSpatialRepository;
    private final DeviceCurrentStateRepository deviceCurrentStateRepository;
    private final RouteCampaignCandidateRepository routeCampaignCandidateRepository;
    private final SegmentHistoricalStatsRepository segmentHistoricalStatsRepository;
    private final CampaignRepository campaignRepository;
    private final PeriodicAllocationRepository periodicAllocationRepository;
    private final DemoPeriodicAllocationRepository demoPeriodicAllocationRepository;

    @Autowired
    private  AllocationCostCalculator allocationCostCalculator;


    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())//needed so that time can be saved in jsonb module
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);


    public record EligiblePair(String deviceId, String campaignId, double cost, Instant predictedEntryTime) {}
    public record AllocationResult(String deviceId, String campaignId, int allocatedPlays) {}



    private final GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);

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
                         RouteCampaignCandidateRepository routeCampaignCandidateRepository,
                         SegmentHistoricalStatsRepository segmentHistoricalStatsRepository,
                         CampaignRepository campaignRepository,
                         PeriodicAllocationRepository periodicAllocationRepository,
                         DemoPeriodicAllocationRepository demoPeriodicAllocationRepository,
                         EntityManager entityManager) {
        this.deviceCredentialRepository = deviceCredentialRepository;
        this.geoLocationRepository = geoLocationRepository;
        this.heartbeatRepository = heartbeatRepository;
        this.imageRepository = imageRepository;
        this.deviceTaskRepository = deviceTaskRepository;
        this.deviceRouteRepository = deviceRouteRepository;
        this.routeStopsRepository = routeStopsRepository;
        this.routeSpatialRepository = routeSpatialRepository;
        this.deviceCurrentStateRepository = deviceCurrentStateRepository;
        this.routeCampaignCandidateRepository = routeCampaignCandidateRepository;
        this.campaignRepository = campaignRepository;
        this.segmentHistoricalStatsRepository = segmentHistoricalStatsRepository;
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

        // D1 — pull active campaigns
        List<Campaign> campaigns = campaignRepository.findAllCampaignsWindow(startWindow, endWindow);;

        List<String> campaignIds = campaigns.stream().map(Campaign::getId).toList();

        // D2 — pull candidate routes for those campaigns (pre-pruned static geometry matches)
        List<RouteCampaignCandidateEntity> routeCandidates =
                routeCampaignCandidateRepository.findActiveRouteMatchesForCampaignIds(campaignIds);
        List<Long> routeIds = routeCandidates.stream().map(RouteCampaignCandidateEntity::getRouteId).toList();

        // D3 — resolve ALL devices currently active on each candidate route (not just one)
        List<DeviceRouteEntity> devicesOnRoutes = deviceRouteRepository.findByRouteIdAndActive(routeIds);

        // group by routeId -> list of devices (a route can have multiple concurrently active devices)
        Map<Long, List<DeviceRouteEntity>> routeIdToDevices = devicesOnRoutes.stream()
                .collect(Collectors.groupingBy(ds -> ds.getRoute().getRouteId()));

        // D4 — pull live state for all resolved devices
        List<String> deviceIds = devicesOnRoutes.stream()
                .map(dr -> dr.getDevice().getDeviceId()).distinct().toList();
        List<DeviceCurrentStateEntity> deviceCurrentStates = deviceCurrentStateRepository.findByDeviceIdIn(deviceIds);
        Map<String, DeviceCurrentStateEntity> deviceCurrentStateMap = deviceCurrentStates.stream()
                .collect(Collectors.toMap(DeviceCurrentStateEntity::getDeviceId, Function.identity()));

        List<EligiblePair> eligiblePairs = new ArrayList<>();

        // D5-D8 — for EVERY device on EVERY candidate route, predict + filter + cost
        for (RouteCampaignCandidateEntity candidate : routeCandidates) {

            List<DeviceRouteEntity> devicesForThisRoute = routeIdToDevices.get(candidate.getRouteId());
            if (devicesForThisRoute == null || devicesForThisRoute.isEmpty()) continue; // no device on this route right now

            Campaign campaign = campaigns.stream()
                    .filter(c -> c.getId().equals(candidate.getCampaignId()))
                    .findFirst().orElseThrow();

            // iterate every device on this route — not just the first/only one
            for (DeviceRouteEntity deviceRoute : devicesForThisRoute) {

                String deviceId = deviceRoute.getDevice().getDeviceId();
                DeviceCurrentStateEntity currentState = deviceCurrentStateMap.get(deviceId);
                if (currentState == null) continue; // no live state yet for this device

                double currentDistance = currentState.getDistanceAlongRoute();
                double smoothedDelaySeconds = currentState.getSmoothedDelay();

                // D5 — historical stats for segments between current position and entry/exit markers
                List<SegmentHistoricalStatsEntity> segmentStats = segmentHistoricalStatsRepository
                        .findSegmentsBetween(candidate.getRouteId(), currentDistance, candidate.getExitMarker(),
                                getHourOfDay(startWindow), getDayOfWeek(startWindow));

                double totalMeanSeconds = segmentStats.stream().mapToDouble(SegmentHistoricalStatsEntity::getMeanTime).sum();
                double totalVariance = segmentStats.stream().mapToDouble(SegmentHistoricalStatsEntity::getVariance).sum();
                double confidenceBandSeconds = Math.sqrt(totalVariance);

                // D6 — (optional live traffic correction omitted here, would override nearest segment's mean)

                // D7 — predicted arrival window
                Instant predictedEntryTime = Instant.now()
                        .plusSeconds((long) smoothedDelaySeconds)
                        .plusSeconds((long) totalMeanSeconds);

                // D8 — filter: does predicted window overlap this hour?
                boolean overlaps = !predictedEntryTime.isAfter(endWindow)
                        && !predictedEntryTime.plusSeconds((long) confidenceBandSeconds).isBefore(startWindow);

                if (!overlaps) continue;

                double distanceCost = allocationCostCalculator.computeDistanceCost(currentState, campaign);
                double urgencyFactor = allocationCostCalculator.computeUrgencyFactor(campaign);
//                double dwellPenalty = allocationCostCalculator.computeDwellPenalty(candidate, currentState, campaign);
                double confidencePenalty = totalMeanSeconds == 0 ? 0 : confidenceBandSeconds / totalMeanSeconds;

                double cost = distanceCost + urgencyFactor  + confidencePenalty;

                eligiblePairs.add(new EligiblePair(deviceId, campaign.getId(), cost, predictedEntryTime));
            }
        }

// //todo : when we have dedficit ledger
// D9 — tiering: pull escalated/rescheduled campaigns
//        Set<String> tier1CampaignIds = deficitLedgerRepository.findCampaignIdsByStatus("escalated_reschedule");
//
//        List<EligiblePair> tier1Pairs = eligiblePairs.stream()
//                .filter(p -> tier1CampaignIds.contains(p.campaignId())).toList();
//        List<EligiblePair> tier2Pairs = eligiblePairs.stream()
//                .filter(p -> !tier1CampaignIds.contains(p.campaignId())).toList();
//
//        // D10/D11 — cost already computed above; run allocation solve, Tier 1 first against real capacity
//
//
//        List<AllocationResult> tier1Allocations = allocationCostCalculator.allocateWaterFilling(tier1Pairs, campaigns);
//        List<AllocationResult> tier2Allocations = allocationCostCalculator.allocateWaterFilling(tier2Pairs, campaigns);
//
//        List<AllocationResult> allAllocations = new ArrayList<>();
//        allAllocations.addAll(tier1Allocations);
//        allAllocations.addAll(tier2Allocations);

        List<AllocationResult> allAllocations = allocationCostCalculator.allocateMinCostFlow(eligiblePairs, campaigns);

        List<PeriodicAllocationEntity>periodicAllocationEntities = new ArrayList<>();

        // D12 — persist allocations
        for (AllocationResult result : allAllocations) {
            PeriodicAllocationEntity allocation = new PeriodicAllocationEntity();
            allocation.setDeviceId(result.deviceId());
            allocation.setCampaignId(result.campaignId());
            allocation.setStartWindow(startWindow);
            allocation.setEndWindow(endWindow);
            allocation.setAllocatedPlays(result.allocatedPlays());
            ZoneId zoneId = ZoneId.of("Asia/Kolkata");
            allocation.setDateOfAllocation(endWindow.atZone(zoneId).toLocalDate());
            allocation.setLocalStartTime(startWindow.atZone(zoneId).toLocalTime());
            allocation.setLocalEndTime(endWindow.atZone(zoneId).toLocalTime());

            periodicAllocationEntities.add(allocation);

        }

        if(demoModeForFullDay){
            demoPeriodicAllocationRepository.saveAll(periodicAllocationEntities);
        }
        else{
            //not a demo - hourly cycle
            periodicAllocationRepository.saveAll(periodicAllocationEntities);
        }

        // D13 — update campaign budgets
        Map<String, Integer> playsByCampaign = periodicAllocationEntities.stream()
                .collect(Collectors.groupingBy(
                        PeriodicAllocationEntity::getCampaignId,
                        Collectors.summingInt(PeriodicAllocationEntity::getAllocatedPlays)));

        for (Campaign campaign : campaigns) {
            Integer playsThisHour = playsByCampaign.getOrDefault(campaign.getId(), 0);
            campaign.setImpressions(campaign.getImpressions() - playsThisHour);
            campaignRepository.save(campaign);
        }

        //todo : decide how to deal with deficit : D14 — log deficits for unmet demand
//        for (Campaign campaign : campaigns) {
//            int playsNeeded = computePlaysNeeded(campaign);
//            int playsAllocated = playsByCampaign.getOrDefault(campaign.getId(), 0);
//            int deficit = playsNeeded - playsAllocated;
//
//            if (deficit > 0) {
//                DeficitLedgerEntity ledger = deficitLedgerRepository
//                        .findByCampaignIdAndHourWindowStart(campaign.getId(), startWindow)
//                        .orElseGet(DeficitLedgerEntity::new);
//
//                ledger.setCampaignId(campaign.getId());
//                ledger.setHourWindowStart(startWindow);
//                ledger.setPlaysNeeded(playsNeeded);
//                ledger.setPlaysAllocated(playsAllocated);
//                ledger.setDeficit(deficit);
//
//                boolean hadDeviceCandidate = eligiblePairs.stream()
//                        .anyMatch(p -> p.campaignId().equals(campaign.getId()));
//                ledger.setReason(hadDeviceCandidate ? "starved_despite_availability" : "no_device_in_region");
//
//                int graceCycles = ledger.getGraceCyclesUsed() == null ? 0 : ledger.getGraceCyclesUsed();
//                if (graceCycles >= GRACE_CYCLE_LIMIT) {
//                    ledger.setStatus("paused_awaiting_decision");
//                    campaign.setStatus("paused_awaiting_decision");
//                    campaignRepository.save(campaign);
//                    // trigger advertiser notification — async, outside this flow
//                } else {
//                    ledger.setGraceCyclesUsed(graceCycles + 1);
//                    ledger.setStatus("auto_retry");
//                }
//                deficitLedgerRepository.save(ledger);
//            }
//        }
    }

    private int getHourOfDay(Instant instant) {
        return instant.atZone(ZoneOffset.UTC).getHour();
    }

    private int getDayOfWeek(Instant instant) {
        return instant.atZone(ZoneOffset.UTC).getDayOfWeek().getValue(); // 1 = Monday ... 7 = Sunday
    }



    @Transactional
    public DeviceRouteEntity assignRouteToDevice(String deviceId, Long routeId) {

        // Use getReference to avoid an extra SELECT query; sets up proxies for the foreign keys
        DeviceCredentialEntity deviceProxy = entityManager.getReference(DeviceCredentialEntity.class, deviceId);
        RouteEntity routeProxy = entityManager.getReference(RouteEntity.class, routeId);

        Pair<String, String>locations = extractPoints(routeProxy.getPath());

        DeviceRouteEntity assignment = new DeviceRouteEntity();
        assignment.setDevice(deviceProxy);
        assignment.setRoute(routeProxy);
        assignment.setFromLocation(locations.getLeft());
        assignment.setToLocation(locations.getRight());
        assignment.setActive(true);

        return deviceRouteRepository.save(assignment);
    }

    private Pair<String, String> extractPoints(LineString path) {
        if (path == null || path.isEmpty()) {
            return Pair.of(null, null);
        }
        Point startPoint = path.getStartPoint();
        Point endPoint = path.getEndPoint();

        return Pair.of(startPoint.toString(), endPoint.toString());
    }



}
