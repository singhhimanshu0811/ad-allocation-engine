package com.routeiq.device.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "devices_tasks")
public class DeviceTaskEntity extends AuditableEntity {

    @Id
    @Column(name = "device_id", nullable = false, length = 16)
    private String deviceId;

    @Column(name = "poll_interval_seconds", nullable = false)
    private int pollIntervalSeconds;

    @Column(nullable = false, length = 20)
    private String task;

    protected DeviceTaskEntity() {
    }

    public DeviceTaskEntity(String deviceId, int pollIntervalSeconds, String task) {
        this.deviceId = deviceId;
        this.pollIntervalSeconds = pollIntervalSeconds;
        this.task = task;
    }

    public String getTask() {
        return task;
    }

    public int getPollIntervalSeconds() {
        return pollIntervalSeconds;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public void setPollIntervalSeconds(int pollIntervalSeconds) {
        this.pollIntervalSeconds = pollIntervalSeconds;
    }
}
