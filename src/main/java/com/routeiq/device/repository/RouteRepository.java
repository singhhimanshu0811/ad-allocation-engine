package com.routeiq.device.repository;

import com.routeiq.device.entity.RouteEntity;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RouteRepository extends JpaRepository<RouteEntity, Long> {

    Optional<RouteEntity> findByRouteId(Long routeId);

    Optional<RouteEntity> findByRouteName(String routeName);

    @Query(value = """
    SELECT *
    FROM routes
    ORDER BY path <-> ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)
    LIMIT 1
    """, nativeQuery = true)
    Optional<RouteEntity> findNearestRoute(
            @Param("lat") double lat,
            @Param("lon") double lon
    );
}
