package com.routeiq.device.repository;

import com.routeiq.device.entity.DeviceTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceTaskRepository extends JpaRepository<DeviceTaskEntity, String> {
}
