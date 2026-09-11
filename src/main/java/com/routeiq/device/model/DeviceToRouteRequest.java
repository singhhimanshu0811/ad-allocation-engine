package com.routeiq.device.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Setter;

public record DeviceToRouteRequest(@NotNull @NotBlank String deviceId,
                                   @NotNull Long routeId) {

}
