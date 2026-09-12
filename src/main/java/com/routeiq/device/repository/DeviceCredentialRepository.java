package com.routeiq.device.repository;

import com.routeiq.device.entity.DeviceCredentialEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeviceCredentialRepository extends JpaRepository<DeviceCredentialEntity, String> {

    @Query("select d  from DeviceCredentialEntity d where d.enabled = true ")
    List<DeviceCredentialEntity>findAllActiveDevices();
}
