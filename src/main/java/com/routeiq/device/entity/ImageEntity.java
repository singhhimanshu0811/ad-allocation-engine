package com.routeiq.device.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "images", indexes = {
        @Index(name = "idx_images_device_timestamp", columnList = "device_id,timestamp")
})
public class ImageEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private DeviceCredentialEntity device;

    @Column(name = "image_url", nullable = false, length = 2048)
    private String imageUrl;

    @Column(nullable = false)
    private Instant timestamp;

    protected ImageEntity() {
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
