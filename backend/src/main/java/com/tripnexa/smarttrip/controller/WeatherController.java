package com.tripnexa.smarttrip.controller;

import com.tripnexa.smarttrip.integration.weather.WeatherClient;
import com.tripnexa.smarttrip.integration.weather.WeatherForecast;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/weather")
public class WeatherController {
    private final WeatherClient weatherClient;

    public WeatherController(WeatherClient weatherClient) {
        this.weatherClient = weatherClient;
    }

    @GetMapping
    public WeatherForecast forecast(
        @RequestParam @NotBlank String destination,
        @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return weatherClient.forecast(destination, date);
    }
}
