package com.routeiq.device.config;

import com.routeiq.device.entity.Campaign;
import com.routeiq.device.entity.DeviceCurrentStateEntity;
import com.routeiq.device.entity.RouteCampaignCandidateEntity;
import com.routeiq.device.service.DeviceService;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Component
public class AllocationCostCalculator {

    private static final double W1_DISTANCE = 1.0;
    private static final double W2_URGENCY = 1.0;//for budget = urgency = budget / secondsToDeadline, so higher urgency -> lower cost
    private static final double W3_DWELL = 1.0;
    private static final double W4_CONFIDENCE = 1.0;

    private static final Integer AD_DURATION_SECONDS = 60; // default ad duration in seconds

    private Integer DEVICE_HOURLY_CAPACITY_SECONDS = 3600; // 1 hour in seconds - WINDOW SIZE IS 1 HOUR

    private final GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);


    public double computeDistanceCost(DeviceCurrentStateEntity currentState, Campaign campaign) {
        Point campaignCenter = gf.createPoint(new Coordinate(campaign.getLongitude(), campaign.getLatitude()));
        double distanceMeters = haversineDistance(
                currentState.getPosition(), campaignCenter);

        return W1_DISTANCE * (distanceMeters / (campaign.getRadiusKm() * 1000));//in metres
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


    public List<DeviceService.AllocationResult> allocateWaterFilling(List<DeviceService.EligiblePair> tier1Pairs, List<Campaign> campaigns) {
        //todo : complete this function
        return List.of();
    }
}
