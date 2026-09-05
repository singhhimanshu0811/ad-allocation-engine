package com.routeiq.device.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "devices_credential")
public class DeviceCredentialEntity extends AuditableEntity {

    @Id
    @Column(name = "device_id", nullable = false, length = 16)
    private String deviceId;

    @Column(name = "secret_key", nullable = false, length = 256)
    private String secretKey;

    @Column(nullable = false)
    private boolean enabled;

    protected DeviceCredentialEntity() {
    }

    public String getSecretKey() {
        return secretKey;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
