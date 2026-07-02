package com.tripnexa.smarttrip.integration.weather;

import java.time.LocalDate;
import java.util.List;

public record WeatherForecast(
    String destination,
    String resolvedLocation,
    LocalDate date,
    double minimumTemperatureCelsius,
    double maximumTemperatureCelsius,
    int averageHumidityPercent,
    int precipitationProbabilityPercent,
    String condition,
    List<String> warnings
) {}
