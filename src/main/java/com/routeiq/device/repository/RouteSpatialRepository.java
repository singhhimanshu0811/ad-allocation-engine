package com.routeiq.device.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RouteSpatialRepository {
    @Query(value = """
        SELECT ST_LineLocatePoint(r.path::geometry, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326))
               * ST_Length(r.path::geography)
        FROM routes r WHERE r.id = :routeId
        """, nativeQuery = true)
    Double locateDistanceAlongRoute(@Param("routeId") Long routeId,
                                    @Param("lat") double lat,
                                    @Param("lon") double lon);
}
