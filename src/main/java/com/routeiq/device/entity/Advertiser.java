package com.routeiq.device.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "advertisers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_advertisers_name", columnNames = "name"),
                @UniqueConstraint(name = "uk_advertisers_email", columnNames = "email")
        },
        indexes = {
                @Index(name = "idx_advertisers_name", columnList = "name"),
                @Index(name = "idx_advertisers_email", columnList = "email")
        }
)
@Getter
@Setter
public class Advertiser {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    private String phone;
    private String companyName;
    private String website;
    private String address1;
    private String address2;
    private String landmark;
    private String city;
    private String state;
    private String pincode;

    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
