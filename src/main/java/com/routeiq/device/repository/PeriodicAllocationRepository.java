package com.routeiq.device.repository;

import com.routeiq.device.entity.PeriodicAllocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PeriodicAllocationRepository extends JpaRepository<PeriodicAllocationEntity, String> {

}
