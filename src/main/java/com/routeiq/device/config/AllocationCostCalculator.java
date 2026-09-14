package com.routeiq.device.config;

import com.routeiq.device.entity.Campaign;
import com.routeiq.device.entity.DeviceCurrentStateEntity;
import com.routeiq.device.entity.RouteCampaignCandidateEntity;
import com.routeiq.device.repository.RouteCampaignCandidateRepository;
import com.routeiq.device.service.DeviceService.EligiblePair;
import com.routeiq.device.service.DeviceService.AllocationResult;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Component
public class AllocationCostCalculator {

    private static final double W1_DISTANCE = 1.0;
    private static final double W2_URGENCY = 1.0;//for budget = urgency = budget / secondsToDeadline, so higher urgency -> lower cost
    private static final double W3_DWELL = 1.0;
    private static final double W4_CONFIDENCE = 1.0;

    private static final Integer AD_DURATION_SECONDS = 60; // default ad duration in seconds

    private static final boolean REGION_CONSTRAINT = false;

    private Integer MAXIMUM_ALLOWED_TIME_FOR_SINGLE_AD = 0;

    double[] tierMultipliers = {1.0, 1.5, 2.5, 5.0};
    //TODO: USE THIS TO ENSURE ONE AD DOESNT STARVE OTHERS . RIGHT NOW SET TO 0. TO BE SET BY CONFIG WHEN NEED

    private static Long COST_SCALE = 1000L;

    private final GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);


    public double computeDistanceCost(RouteCampaignCandidateEntity campaignCandidateEntity, Campaign campaign) {
        //perpendicular intercept on route from center of campaign
        return W1_DISTANCE * (campaignCandidateEntity.getDistanceOfEntryMarkerFromCenter() / (campaign.getRadiusKm() * 1000));
    }

    public double computeUrgencyFactor(Campaign campaign) {
        if(Objects.nonNull(campaign.getEndInstant())){
            long secondsToDeadline = Duration.between(Instant.now(), campaign.getEndInstant()).getSeconds();
            if (secondsToDeadline <= 0) secondsToDeadline = 1; // avoid divide-by-zero, already overdue
            double urgency = campaign.getBudget() / secondsToDeadline;
            return W2_URGENCY * (1.0 / (1.0 + urgency)); // higher urgency -> lower cost
        }
       else{
           //when there is no time filter - only budget matters, so we can return a constant cost factor
              return W2_URGENCY * (1.0 / (1.0 + campaign.getBudget()));
        }
    }
//todo : needed when we have long video ads - is this device in this region long enough to play full ad
//    public double computeDwellPenalty(RouteCampaignCandidateEntity candidate,
//                                      DeviceCurrentStateEntity currentState,
//                                      Campaign campaign) {
//        double regionSpan = candidate.getExitMarker() - candidate.getEntryMarker();
//        double minDwellNeeded = AD_DURATION_SECONDS; // simplification: needs at least one ad-length of dwell
//        // predicted dwell time approximated via historical pace assumption — refine later with real segment speed
//        double predictedDwellSeconds = regionSpan > 0 ? regionSpan / assumedSpeedMetersPerSecond() : 0;
//        double shortfall = Math.max(0, minDwellNeeded - predictedDwellSeconds);
//        return W3_DWELL * (minDwellNeeded == 0 ? 0 : shortfall / minDwellNeeded);
//    }

    public double computeConfidencePenalty(double confidenceBandSeconds, double totalMeanSeconds) {
        if (totalMeanSeconds == 0) return 0;
        return W4_CONFIDENCE * (confidenceBandSeconds / totalMeanSeconds);
    }

    private double haversineDistance(Point p1, Point p2) {
        double lat1 = p1.getY();  // JTS: Y = latitude
        double lon1 = p1.getX();  // JTS: X = longitude
        double lat2 = p2.getY();
        double lon2 = p2.getX();

        double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }


    public List<AllocationResult> allocateMinCostFlow(List<EligiblePair> pairs,
                                                      List<Campaign> campaigns, Long duration
                                                                     ) {
        if (pairs.isEmpty()) return List.of();

        List<String> campaignIds = pairs.stream().map(EligiblePair::campaignId).distinct().toList();
        List<String> deviceIds = pairs.stream().map(EligiblePair::deviceId).distinct().toList();

        Map<String, Integer> campaignIndex = new HashMap<>();
        for (int i = 0; i < campaignIds.size(); i++) campaignIndex.put(campaignIds.get(i), i);
        Map<String, Integer> deviceIndex = new HashMap<>();
        for (int i = 0; i < deviceIds.size(); i++) deviceIndex.put(deviceIds.get(i), i);

        int source = 0;
        int campaignOffset = 1;
        int deviceOffset = campaignOffset + campaignIds.size();
        int sink = deviceOffset + deviceIds.size();

        MinCostFlow mcf = new MinCostFlow(sink + 1);
//source edges
        for (String campaignId : campaignIds) {
            Campaign c = campaigns.stream().filter(camp -> camp.getId().equals(campaignId)).findFirst().orElseThrow();
           // approach 1 :  mcf.addEdge(source, campaignOffset + campaignIndex.get(campaignId), c.getImpressions(), 0);

            long tierSize = c.getImpressions() / tierMultipliers.length;

            if(tierSize == 0){
                mcf.addEdge(source, campaignOffset + campaignIndex.get(campaignId), c.getImpressions(), 0);
            }
            else{
                for (double multiplier : tierMultipliers) {

                    mcf.addEdge(source, campaignOffset + campaignIndex.get(campaignId), tierSize, Math.round(multiplier * COST_SCALE));
                }
                //difference in approach 1 and 2 : make a campaign asigment less and less favourable to be repatedly assigned to a single device,
                //so that all impressions are not consumed in single hour.
                //if we have signle campaign alloted for this device - then wont matter.
            }

        }

        Map<String, List<Integer>> pairEdgeIndex = new HashMap<>();
        for (EligiblePair pair : pairs) {

            long playableSeconds = REGION_CONSTRAINT ? Math.max(0, duration - pair.timeToReach()) : duration;
            //time to reach will be 0 if its already within region

            long capacityPlaysAllowed = playableSeconds / AD_DURATION_SECONDS;

            //if for this pair - some time was remaing - it should not remain empty . some other should be allowed.
            // todo : see how

            if (capacityPlaysAllowed <= 0) continue;

            int from = campaignOffset + campaignIndex.get(pair.campaignId());
            int to = deviceOffset + deviceIndex.get(pair.deviceId());
            long scaledCost = Math.round(pair.cost() * COST_SCALE);


            long capacityPlaysAllowedTiered = capacityPlaysAllowed / tierMultipliers.length;

            String key = pair.campaignId() + "|" + pair.deviceId();

            if(capacityPlaysAllowedTiered == 0){
                int forwardEdgeId = mcf.edgeCount();
                mcf.addEdge(from, to, capacityPlaysAllowed, scaledCost);
                pairEdgeIndex.put(key, List.of(forwardEdgeId));
            }

            else{

                List<Integer> tieredForwardEdgeIds = new ArrayList<>();
                for(double multiplier : tierMultipliers){
                    int forwardEdgeId = mcf.edgeCount();
                    mcf.addEdge(from, to, capacityPlaysAllowedTiered, Math.round(scaledCost * multiplier));
                    tieredForwardEdgeIds.add(forwardEdgeId);
                }
                pairEdgeIndex.put(key, tieredForwardEdgeIds);
            }

//            mcf.addEdge(from, to, capacityPlaysAllowed, scaledCost);
//            pairEdgeIndex.put(pair.campaignId() + "|" + pair.deviceId(), forwardEdgeId);
        }
//sin edges
        for (String deviceId : deviceIds) {
            long deviceCapacityPlays = (long) (duration / AD_DURATION_SECONDS);
            mcf.addEdge(deviceOffset + deviceIndex.get(deviceId), sink, deviceCapacityPlays, 0);
        }

        mcf.run(source, sink);

        List<AllocationResult> results = new ArrayList<>();
        for (Map.Entry<String, List<Integer>> entry : pairEdgeIndex.entrySet()) {
            String[] parts = entry.getKey().split("\\|");
            String campaignId = String.valueOf(parts[0]);
            String deviceId = parts[1];

            long flow = 0;

            for(Integer e : entry.getValue()){
                flow += mcf.getFlowOnEdge(e);
            }

            if (flow > 0) results.add(new AllocationResult(deviceId, campaignId, (int) flow));
        }
        return results;
    }
}
