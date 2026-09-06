package com.routeiq.device.repository;

import com.routeiq.device.entity.DeviceCredentialEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceCredentialRepository extends JpaRepository<DeviceCredentialEntity, String> {
}
