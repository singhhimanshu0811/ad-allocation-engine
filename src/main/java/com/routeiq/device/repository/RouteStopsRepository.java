package com.routeiq.device.repository;

import com.routeiq.device.entity.RouteStopEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteStopsRepository extends JpaRepository<RouteStopEntity, Long> {

    List<RouteStopEntity> findByRouteIdOrderByStopSequenceNumberAsc(Long routeId);

}
