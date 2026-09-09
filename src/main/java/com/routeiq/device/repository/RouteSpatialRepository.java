package com.routeiq.device.repository;

import com.routeiq.device.entity.RouteEntity;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RouteSpatialRepository extends JpaRepository<RouteEntity, Long> {
    @Query(value = """
        SELECT ST_LineLocatePoint(r.path::geometry, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326))
               * ST_Length(r.path::geography)
        FROM routes r WHERE r.id = :routeId
        """, nativeQuery = true)
    Double locateDistanceAlongRoute(@Param("routeId") Long routeId,
                                    @Param("lat") double lat,
                                    @Param("lon") double lon);
}
