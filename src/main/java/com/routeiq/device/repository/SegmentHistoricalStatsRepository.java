package com.routeiq.device.repository;

import com.routeiq.device.entity.SegmentHistoricalStatsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SegmentHistoricalStatsRepository extends JpaRepository<SegmentHistoricalStatsEntity, Long> {
    Optional<SegmentHistoricalStatsEntity> findByRouteIdAndSegmentStartDistanceAndSegmentEndDistanceAndHourOfDayAndDayOfWeek(
            Long routeId, Double segmentStartDistance, Double segmentEndDistance, Integer hourOfDay, Integer dayOfWeek);

    @Query("""
        SELECT s FROM SegmentHistoricalStatsEntity s
        WHERE s.routeId = :routeId
          AND s.segmentStartDistance >= :fromDistance
          AND s.segmentEndDistance <= :toDistance
          AND s.hourOfDay = :hourOfDay
          AND s.dayOfWeek = :dayOfWeek
        ORDER BY s.segmentStartDistance ASC
        """)
    List<SegmentHistoricalStatsEntity> findSegmentsBetween(@Param("routeId") Long routeId,
                                                           @Param("fromDistance") double fromDistance,
                                                           @Param("toDistance") double toDistance,
                                                           @Param("hourOfDay") int hourOfDay,
                                                           @Param("dayOfWeek") int dayOfWeek);
}
