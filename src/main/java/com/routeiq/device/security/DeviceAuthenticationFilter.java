package com.routeiq.device.security;

import com.routeiq.device.entity.DeviceCredentialEntity;
import com.routeiq.device.repository.DeviceCredentialRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class DeviceAuthenticationFilter extends OncePerRequestFilter {

    private static final String DEVICE_ID_HEADER = "X-Device-Id";
    private static final String DEVICE_KEY_HEADER = "X-Device-Key";

    private final DeviceCredentialRepository credentialRepository;
    private final ConcurrentMap<String, CachedCredential> credentialCache = new ConcurrentHashMap<>();
    private final Duration cacheTtl;

    public DeviceAuthenticationFilter(
            DeviceCredentialRepository credentialRepository,
            @Value("${device.auth.cache-ttl-ms:3600000}") long cacheTtlMs) {
        this.credentialRepository = credentialRepository;
        if (cacheTtlMs <= 0) {
            throw new IllegalArgumentException("device.auth.cache-ttl-ms must be greater than zero");
        }
        this.cacheTtl = Duration.ofMillis(cacheTtlMs);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String deviceId = request.getHeader(DEVICE_ID_HEADER);
        String providedKey = request.getHeader(DEVICE_KEY_HEADER);

        if (deviceId == null || deviceId.isBlank()
                || providedKey == null || providedKey.isBlank()
                || !isValidCredential(deviceId, providedKey)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid device credentials");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isValidCredential(String deviceId, String providedKey) {
        CachedCredential cached = credentialCache.get(deviceId);
        if (cached == null || cached.expiresAt().isBefore(Instant.now())) {
            cached = loadCredential(deviceId);
        }

        return cached != null
                && cached.enabled()
                && MessageDigest.isEqual(
                        cached.secretKey().getBytes(StandardCharsets.UTF_8),
                        providedKey.getBytes(StandardCharsets.UTF_8)
                );
    }

    private CachedCredential loadCredential(String deviceId) {
        CachedCredential loaded = credentialRepository.findById(deviceId)
                .filter(DeviceCredentialEntity::isEnabled)
                .map(credential -> new CachedCredential(
                        credential.getSecretKey(),
                        true,
                        Instant.now().plus(cacheTtl)
                ))
                .orElse(null);

        if (loaded == null) {
            credentialCache.remove(deviceId);
        } else {
            credentialCache.put(deviceId, loaded);
        }
        return loaded;
    }

    private record CachedCredential(String secretKey, boolean enabled, Instant expiresAt) {
    }
}
