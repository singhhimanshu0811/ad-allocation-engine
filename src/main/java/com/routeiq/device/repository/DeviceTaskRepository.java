package com.routeiq.device.repository;

import com.routeiq.device.entity.DeviceTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceTaskRepository extends JpaRepository<DeviceTaskEntity, String> {
}
