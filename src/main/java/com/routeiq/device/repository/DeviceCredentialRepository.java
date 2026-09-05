package com.routeiq.device.repository;

import com.routeiq.device.entity.DeviceCredentialEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceCredentialRepository extends JpaRepository<DeviceCredentialEntity, String> {
}
