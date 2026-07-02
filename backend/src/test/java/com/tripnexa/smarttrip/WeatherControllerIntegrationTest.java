package com.tripnexa.smarttrip;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tripnexa.smarttrip.integration.weather.WeatherClient;
import com.tripnexa.smarttrip.integration.weather.WeatherForecast;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class WeatherControllerIntegrationTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean WeatherClient weatherClient;

    @Test
    @WithMockUser
    void returnsWeatherForecastFromStandaloneEndpoint() throws Exception {
        LocalDate date = LocalDate.of(2026, 7, 3);
        when(weatherClient.forecast("Manali", date)).thenReturn(new WeatherForecast(
            "Manali", "Manali, Himachal Pradesh, IN", date, 18.5, 25.2,
            72, 65, "light rain", List.of("Keep indoor alternatives ready")));

        mockMvc.perform(get("/api/weather").param("destination", "Manali").param("date", date.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.destination").value("Manali"))
            .andExpect(jsonPath("$.precipitationProbabilityPercent").value(65));
    }

    @Test
    void endpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/weather").param("destination", "Manali").param("date", "2026-07-03"))
            .andExpect(status().isUnauthorized());
    }
}
