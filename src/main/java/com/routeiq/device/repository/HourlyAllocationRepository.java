package com.routeiq.device.repository;

import com.routeiq.device.entity.HourlyAllocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HourlyAllocationRepository extends JpaRepository<HourlyAllocationEntity, String> {

}
