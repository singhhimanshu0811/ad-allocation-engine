package com.routeiq.device.config;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class DelayKalmanFilter {
    private double estimate;
    private double errorCovariance;
    private double processNoise;
    private double measurementNoise;



    public double update(double measuredDelay) {
        double predictedEstimate = estimate;
        double predictedCovariance = errorCovariance + processNoise;

        double kalmanGain = predictedCovariance / (predictedCovariance + measurementNoise);
        estimate = predictedEstimate + kalmanGain * (measuredDelay - predictedEstimate);
        errorCovariance = (1 - kalmanGain) * predictedCovariance;

        return estimate;
    }

    public double getErrorCovariance() { return errorCovariance; }
}
