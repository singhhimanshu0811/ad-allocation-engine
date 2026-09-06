package com.routeiq.device.repository;

import com.routeiq.device.entity.RouteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RouteRepository extends JpaRepository<RouteEntity, Long> {

    Optional<RouteEntity> findByRouteId(Long routeId);

    Optional<RouteEntity> findByRouteName(String routeName);
}
