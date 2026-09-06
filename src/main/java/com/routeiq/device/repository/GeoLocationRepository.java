package com.routeiq.device.repository;

import com.routeiq.device.entity.GeoLocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GeoLocationRepository extends JpaRepository<GeoLocationEntity, Long> {

    List<GeoLocationEntity> findByCaptureSessionIdOrderByTimestampAsc(UUID captureSessionId);
}
