package com.routeiq.device.repository;

import com.routeiq.device.entity.DeviceCurrentStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceCurrentStateRepository extends JpaRepository<DeviceCurrentStateEntity, String> {
}
