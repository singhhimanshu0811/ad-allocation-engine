package com.routeiq.device.repository;

import com.routeiq.device.entity.GeoLocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeoLocationRepository extends JpaRepository<GeoLocationEntity, Long> {
}
