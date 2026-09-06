package com.routeiq.device.repository;

import com.routeiq.device.entity.RouteStopEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteStopsRepository extends JpaRepository<RouteStopEntity, Long> {

    List<RouteStopEntity> findByRouteIdOrderBySequenceNumberAsc(Long routeId);

}
