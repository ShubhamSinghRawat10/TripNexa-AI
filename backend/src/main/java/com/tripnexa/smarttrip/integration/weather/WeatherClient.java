package com.tripnexa.smarttrip.integration.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tripnexa.smarttrip.config.WeatherProperties;
import com.tripnexa.smarttrip.exception.BadRequestException;
import com.tripnexa.smarttrip.exception.ExternalServiceException;
import com.tripnexa.smarttrip.exception.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class WeatherClient {
    private final WeatherProperties properties;
    private final RestClient restClient;
    private final Clock clock;

    public WeatherClient(WeatherProperties properties, RestClient openWeatherRestClient, Clock clock) {
        this.properties = properties;
        this.restClient = openWeatherRestClient;
        this.clock = clock;
    }

    public WeatherForecast forecast(String destination, LocalDate date) {
        validateRequest(destination, date);
        try {
            GeoLocation location = geocode(destination.trim());
            ForecastResponse response = restClient.get()
                .uri(uri -> uri.path("/data/2.5/forecast")
                    .queryParam("lat", location.lat())
                    .queryParam("lon", location.lon())
                    .queryParam("units", "metric")
                    .queryParam("appid", properties.getApiKey())
                    .build())
                .retrieve()
                .body(ForecastResponse.class);
            return aggregate(destination.trim(), date, location, response);
        } catch (ResourceNotFoundException | BadRequestException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new ExternalServiceException("OpenWeather request failed", exception);
        }
    }

    private GeoLocation geocode(String destination) {
        GeoLocation[] locations = restClient.get()
            .uri(uri -> uri.path("/geo/1.0/direct")
                .queryParam("q", destination + ",IN")
                .queryParam("limit", 1)
                .queryParam("appid", properties.getApiKey())
                .build())
            .retrieve()
            .body(GeoLocation[].class);
        if (locations == null || locations.length == 0) {
            throw new ResourceNotFoundException("Weather location not found: " + destination);
        }
        return locations[0];
    }

    private WeatherForecast aggregate(String destination, LocalDate date, GeoLocation location,
                                      ForecastResponse response) {
        if (response == null || response.list() == null) {
            throw new ExternalServiceException("OpenWeather returned an empty forecast");
        }
        int timezone = response.city() == null ? 0 : response.city().timezone();
        List<ForecastItem> items = response.list().stream()
            .filter(item -> Instant.ofEpochSecond(item.timestamp())
                .atOffset(ZoneOffset.ofTotalSeconds(timezone)).toLocalDate().equals(date))
            .toList();
        if (items.isEmpty()) {
            throw new BadRequestException("Forecast is unavailable for " + date
                + "; OpenWeather supports approximately the next five days");
        }

        double min = items.stream().mapToDouble(item -> item.main().minimum()).min().orElseThrow();
        double max = items.stream().mapToDouble(item -> item.main().maximum()).max().orElseThrow();
        int humidity = (int) Math.round(items.stream().mapToInt(item -> item.main().humidity()).average().orElse(0));
        int precipitation = (int) Math.round(items.stream().mapToDouble(ForecastItem::precipitation).max().orElse(0) * 100);
        String condition = items.stream()
            .flatMap(item -> item.weather() == null ? java.util.stream.Stream.empty() : item.weather().stream())
            .map(WeatherCondition::description)
            .filter(value -> value != null && !value.isBlank())
            .max(Comparator.comparingInt(value -> frequency(items, value)))
            .orElse("Unknown");

        List<String> warnings = new ArrayList<>();
        if (precipitation >= 60) warnings.add("High chance of precipitation; keep indoor alternatives ready");
        String normalized = condition.toLowerCase(Locale.ROOT);
        if (normalized.contains("thunder")) warnings.add("Thunderstorm conditions may disrupt outdoor activities");
        if (normalized.contains("snow")) warnings.add("Snow may affect road and activity access");

        String resolved = location.state() == null
            ? location.name() + ", " + location.country()
            : location.name() + ", " + location.state() + ", " + location.country();
        return new WeatherForecast(destination, resolved, date, round(min), round(max), humidity,
            precipitation, condition, List.copyOf(warnings));
    }

    private int frequency(List<ForecastItem> items, String description) {
        return (int) items.stream()
            .flatMap(item -> item.weather() == null ? java.util.stream.Stream.empty() : item.weather().stream())
            .filter(item -> description.equals(item.description())).count();
    }

    private void validateRequest(String destination, LocalDate date) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new ExternalServiceException("OpenWeather API key is not configured");
        }
        if (destination == null || destination.isBlank()) {
            throw new BadRequestException("Destination is required");
        }
        LocalDate today = LocalDate.now(clock);
        if (date.isBefore(today) || date.isAfter(today.plusDays(5))) {
            throw new BadRequestException("Date must be between " + today + " and " + today.plusDays(5));
        }
    }

    private double round(double value) { return Math.round(value * 10.0) / 10.0; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GeoLocation(String name, double lat, double lon, String country, String state) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ForecastResponse(List<ForecastItem> list, ForecastCity city) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ForecastCity(int timezone) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ForecastItem(
        @JsonProperty("dt") long timestamp,
        ForecastMain main,
        List<WeatherCondition> weather,
        @JsonProperty("pop") double precipitation
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ForecastMain(
        @JsonProperty("temp_min") double minimum,
        @JsonProperty("temp_max") double maximum,
        int humidity
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record WeatherCondition(String description) {}
}
