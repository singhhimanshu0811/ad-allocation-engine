package com.routeiq.device.repository;

import com.routeiq.device.entity.DeviceRouteEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DeviceRouteRepository extends JpaRepository<DeviceRouteEntity, Long> {

    java.util.Optional<DeviceRouteEntity> findByDeviceIdAndFromLocationAndToLocation(
            String deviceId,
            String fromLocation,
            String toLocation
    );

    Optional<DeviceRouteEntity> findByDeviceIdAndActiveTrue(String deviceId);

    @Modifying
    @Query("""
            update DeviceRouteEntity route
            set route.active = false,
                route.updatedAt = CURRENT_TIMESTAMP,
                route.version = route.version + 1
            where route.deviceId = :deviceId
              and route.active = true
            """)
    int deactivateActiveRoute(@Param("deviceId") String deviceId);
}
