package com.routeiq.device.repository;

import com.routeiq.device.entity.HeartbeatEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface HeartbeatRepository extends JpaRepository<HeartbeatEntity, Long> {

    @Modifying
    @Query("delete from HeartbeatEntity heartbeat where heartbeat.receivedAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
