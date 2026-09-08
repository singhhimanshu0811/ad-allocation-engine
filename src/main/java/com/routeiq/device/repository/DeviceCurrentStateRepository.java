package com.routeiq.device.repository;

import com.routeiq.device.entity.DeviceCurrentStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeviceCurrentStateRepository extends JpaRepository<DeviceCurrentStateEntity, String> {

    @Query("SELECT d FROM DeviceCurrentStateEntity d WHERE d.    d.deviceId IN :deviceIds")
    List<DeviceCurrentStateEntity> findByDeviceIdIn(List<String> deviceIds);
}
