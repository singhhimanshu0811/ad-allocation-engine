package com.routeiq.device;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DeviceRestApplication {
    public static void main(String[] args) {
        SpringApplication.run(DeviceRestApplication.class, args);
    }
}
