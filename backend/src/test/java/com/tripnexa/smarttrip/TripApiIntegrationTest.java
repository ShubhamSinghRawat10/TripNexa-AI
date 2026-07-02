package com.tripnexa.smarttrip;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripnexa.smarttrip.repository.TripRepository;
import com.tripnexa.smarttrip.repository.UserRepository;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TripApiIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired TripRepository tripRepository;
    @Autowired UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        tripRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void authenticatedUserCanCreateReadListAndDeleteOwnTrip() throws Exception {
        String token = signUp("Asha", "asha@example.com");
        String tripId = createTrip(token, "Manali");

        mockMvc.perform(get("/api/trip/{id}", tripId).header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.destination").value("Manali"))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.interests[0]").exists());

        mockMvc.perform(put("/api/trip/{id}", tripId)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tripRequest("Shimla"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.destination").value("Shimla"));

        mockMvc.perform(get("/api/trip/history").header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(delete("/api/trip/{id}", tripId).header("Authorization", bearer(token)))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/trip/{id}", tripId).header("Authorization", bearer(token)))
            .andExpect(status().isNotFound());
    }

    @Test
    void tripOwnershipIsNotLeakedAcrossUsers() throws Exception {
        String ownerToken = signUp("Asha", "asha@example.com");
        String otherToken = signUp("Ravi", "ravi@example.com");
        String tripId = createTrip(ownerToken, "Jaipur");

        mockMvc.perform(get("/api/trip/{id}", tripId).header("Authorization", bearer(otherToken)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void protectedAndInvalidRequestsReturnStableJsonErrors() throws Exception {
        mockMvc.perform(get("/api/trip/history"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        String token = signUp("Asha", "asha@example.com");
        Map<String, Object> invalid = Map.of(
            "destination", "", "budget", 0, "days", 0, "people", 0,
            "travelDate", LocalDate.now().minusDays(1).toString(), "interests", java.util.List.of()
        );
        mockMvc.perform(post("/api/trip/create")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.fieldErrors.destination").exists());
    }

    @Test
    void registeredUserCanLoginAndDuplicateSignupIsRejected() throws Exception {
        signUp("Asha", "asha@example.com");
        String login = objectMapper.writeValueAsString(Map.of(
            "email", "ASHA@example.com", "password", "StrongPass123!"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(login))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.accessToken").isNotEmpty());

        String duplicate = objectMapper.writeValueAsString(Map.of(
            "name", "Another Asha", "email", "asha@example.com", "password", "StrongPass123!"));
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON).content(duplicate))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    private String signUp(String name, String email) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
            "name", name, "email", email, "password", "StrongPass123!"));
        String response = mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private String createTrip(String token, String destination) throws Exception {
        Map<String, Object> request = tripRequest(destination);
        String response = mockMvc.perform(post("/api/trip/create")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.get("id").asText();
    }

    private Map<String, Object> tripRequest(String destination) {
        return Map.of(
            "destination", destination,
            "budget", 25000,
            "days", 4,
            "people", 2,
            "travelDate", LocalDate.now().plusMonths(2).toString(),
            "interests", java.util.List.of("Nature", "Food")
        );
    }

    private String bearer(String token) { return "Bearer " + token; }
}
