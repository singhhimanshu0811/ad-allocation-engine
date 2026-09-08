package com.routeiq.device.repository;

import com.routeiq.device.entity.RouteCampaignCandidateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RouteCampaignCandidateRepository extends JpaRepository<RouteCampaignCandidateEntity, Long> {

    @Query("Select rcc from RouteCampaignCandidateEntity rcc where rcc.campaignId IN :campaignId and rcc.active = true")
    List<RouteCampaignCandidateEntity> findActiveRouteMatchesForCampaignIds(List<String> campaignId);
}
