package com.routeiq.device.repository;

import com.routeiq.device.entity.HeartbeatEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface HeartbeatRepository extends JpaRepository<HeartbeatEntity, Long> {

    @Modifying
    @Query("delete from HeartbeatEntity heartbeat where heartbeat.receivedAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);


    List<HeartbeatEntity> findByRouteIdAndReceivedAtBetweenOrderByDeviceIdAscReceivedAtAsc(Long routeId, Instant dayStart, Instant dayEnd);

    @Query("""
    SELECT h FROM HeartbeatEntity h
    WHERE h.deviceId IN :deviceIds
    ORDER BY h.deviceId, h.receivedAt DESC
    """)
    List<HeartbeatEntity> findRecentHeartbeats(List<String> deviceIds);
}
