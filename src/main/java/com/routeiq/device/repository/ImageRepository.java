package com.routeiq.device.repository;

import com.routeiq.device.entity.ImageEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ImageRepository extends JpaRepository<ImageEntity, Long> {

    @Query("""
            select image from ImageEntity image
            where image.device.deviceId = :deviceId
            order by image.timestamp desc
            """)
    List<ImageEntity> findByDeviceId(@Param("deviceId") String deviceId);
}
