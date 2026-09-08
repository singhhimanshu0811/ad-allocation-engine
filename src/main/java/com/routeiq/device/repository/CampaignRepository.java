package com.routeiq.device.repository;

import com.routeiq.device.entity.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface CampaignRepository extends JpaRepository<Campaign, String> {

    @Query("SELECT c FROM Campaign c WHERE  (c.startInstant >= :windowStart AND c.endInstant <= :windowEnd)")
    List<Campaign> findAllCampaignsWindow(@Param("windowStart") Instant windowStart, @Param("windowEnd") Instant windowEnd);
}
