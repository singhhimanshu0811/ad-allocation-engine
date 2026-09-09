package com.routeiq.device.repository;

import com.routeiq.device.entity.DeviceRouteEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRouteRepository extends JpaRepository<DeviceRouteEntity, Long> {

    @Query("""
            select d
            from DeviceRouteEntity d
            where d.device.deviceId = :deviceId and 
                        d.fromLocation = :fromLocation and d.toLocation = :toLocation
              
            """)
    java.util.Optional<DeviceRouteEntity> findByDeviceIdAndFromLocationAndToLocation(
            @Param("deviceId")String deviceId,
            @Param("fromLocation") String fromLocation,
            @Param("toLocation") String toLocation
    );

    @Query("""
            select d
            from DeviceRouteEntity d
            where d.device.deviceId = :deviceId
              
            """)
    Optional<DeviceRouteEntity> findByDeviceIdAndActiveTrue(@Param("deviceId") String deviceId);

    @Query("""
            select d
            from DeviceRouteEntity d
            where d.route.routeId in :routeIds
              and d.active = true
            """)
    List<DeviceRouteEntity> findByRouteIdAndActive(List<Long> routeIds);

    @Modifying
    @Query("""
            update DeviceRouteEntity route
            set route.active = false,
                route.updatedAt = CURRENT_TIMESTAMP,
                route.version = route.version + 1
            where route.device.deviceId = :deviceId
              and route.active = true
            """)
    int deactivateActiveRoute(@Param("deviceId") String deviceId);
}
