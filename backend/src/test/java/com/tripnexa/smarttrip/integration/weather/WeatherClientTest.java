package com.tripnexa.smarttrip.integration.weather;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.tripnexa.smarttrip.config.WeatherProperties;
import com.tripnexa.smarttrip.exception.BadRequestException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class WeatherClientTest {
    private MockRestServiceServer server;
    private WeatherClient client;

    @BeforeEach
    void setUp() {
        WeatherProperties properties = new WeatherProperties();
        properties.setApiKey("test-key");
        properties.setBaseUrl("https://api.openweathermap.org");
        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getBaseUrl());
        server = MockRestServiceServer.bindTo(builder).build();
        Clock clock = Clock.fixed(Instant.parse("2026-07-02T00:00:00Z"), ZoneOffset.UTC);
        client = new WeatherClient(properties, builder.build(), clock);
    }

    @Test
    void geocodesAndAggregatesForecastForRequestedLocalDate() {
        server.expect(once(), requestTo(org.hamcrest.Matchers.containsString("/geo/1.0/direct")))
            .andRespond(withSuccess(
                "[{\"name\":\"Manali\",\"lat\":32.24,\"lon\":77.19,\"country\":\"IN\",\"state\":\"Himachal Pradesh\"}]",
                MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(org.hamcrest.Matchers.containsString("/data/2.5/forecast")))
            .andRespond(withSuccess("""
                {
                  "city": {"timezone": 19800},
                  "list": [
                    {"dt": 1783058400, "main": {"temp_min": 21.0, "temp_max": 24.5, "humidity": 74},
                     "weather": [{"description": "light rain"}], "pop": 0.8},
                    {"dt": 1783080000, "main": {"temp_min": 23.0, "temp_max": 27.0, "humidity": 66},
                     "weather": [{"description": "light rain"}], "pop": 0.4}
                  ]
                }
                """, MediaType.APPLICATION_JSON));

        WeatherForecast result = client.forecast("Manali", LocalDate.of(2026, 7, 3));

        assertThat(result.resolvedLocation()).isEqualTo("Manali, Himachal Pradesh, IN");
        assertThat(result.minimumTemperatureCelsius()).isEqualTo(21.0);
        assertThat(result.maximumTemperatureCelsius()).isEqualTo(27.0);
        assertThat(result.averageHumidityPercent()).isEqualTo(70);
        assertThat(result.precipitationProbabilityPercent()).isEqualTo(80);
        assertThat(result.warnings()).hasSize(1);
        server.verify();
    }

    @Test
    void rejectsDatesOutsideProviderForecastWindowWithoutCallingApi() {
        assertThatThrownBy(() -> client.forecast("Manali", LocalDate.of(2026, 8, 15)))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("between 2026-07-02 and 2026-07-07");
        server.verify();
    }
}
