package com.tripnexa.smarttrip.integration.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripnexa.smarttrip.agent.result.ItineraryActivity;
import com.tripnexa.smarttrip.agent.result.ItineraryDay;
import com.tripnexa.smarttrip.config.GeminiProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Calls Google Gemini API to generate day-wise travel itineraries.
 * Includes retry logic and JSON parsing with fallback.
 */
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final GeminiProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeminiClient(GeminiProperties properties, RestClient geminiRestClient, ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = geminiRestClient;
        this.objectMapper = objectMapper;
    }

    public List<ItineraryDay> generateItinerary(String destination, int days, long activityBudget,
                                                 List<String> interests, List<String> weatherWarnings) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            log.warn("Gemini API key not configured — returning fallback itinerary");
            return fallbackItinerary(destination, days);
        }

        String prompt = buildPrompt(destination, days, activityBudget, interests, weatherWarnings);

        // Try up to 2 times (initial + 1 retry)
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                String responseText = callGemini(prompt);
                List<ItineraryDay> parsed = parseItinerary(responseText);
                if (parsed != null && !parsed.isEmpty()) {
                    return parsed;
                }
                log.warn("Gemini returned empty/unparseable itinerary on attempt {}", attempt + 1);
            } catch (RestClientException e) {
                log.warn("Gemini API call failed on attempt {}: {}", attempt + 1, e.getMessage());
                if (attempt == 0) {
                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        log.warn("All Gemini attempts failed — using fallback itinerary");
        return fallbackItinerary(destination, days);
    }

    private String callGemini(String prompt) {
        var requestBody = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(Map.of("text", prompt)))
            ),
            "generationConfig", Map.of(
                "temperature", 0.7,
                "maxOutputTokens", 4096,
                "responseMimeType", "application/json"
            )
        );

        GeminiResponse response = restClient.post()
            .uri("/v1beta/models/{model}:generateContent?key={key}",
                properties.getModel(), properties.getApiKey())
            .header("Content-Type", "application/json")
            .body(requestBody)
            .retrieve()
            .body(GeminiResponse.class);

        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new RestClientException("Gemini returned no candidates");
        }

        var content = response.candidates().getFirst().content();
        if (content == null || content.parts() == null || content.parts().isEmpty()) {
            throw new RestClientException("Gemini returned empty content");
        }

        return content.parts().getFirst().text();
    }

    private String buildPrompt(String destination, int days, long activityBudget,
                                List<String> interests, List<String> weatherWarnings) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a travel itinerary planning assistant for Indian destinations.\n\n");
        sb.append("Input:\n");
        sb.append("- Destination: ").append(destination).append("\n");
        sb.append("- Days: ").append(days).append("\n");
        sb.append("- Budget remaining for activities: ₹").append(activityBudget).append("\n");
        sb.append("- Interests: ").append(String.join(", ", interests)).append("\n");
        if (weatherWarnings != null && !weatherWarnings.isEmpty()) {
            sb.append("- Weather warnings: ").append(String.join("; ", weatherWarnings)).append("\n");
        }
        sb.append("\nTask: Generate a day-wise itinerary. For each day, list 3-4 activities ");
        sb.append("with estimated time slots and approximate cost in INR. ");
        sb.append("Avoid outdoor activities on days flagged with weather warnings. ");
        sb.append("Include popular local food spots, cultural experiences, and must-visit attractions. ");
        sb.append("Respond ONLY in JSON matching this schema:\n");
        sb.append("[{\"day\": 1, \"activities\": [{\"name\": \"string\", \"startTime\": \"HH:mm\", ");
        sb.append("\"endTime\": \"HH:mm\", \"estimatedCost\": number, \"category\": \"string\"}]}]\n");
        sb.append("Categories: sightseeing, food, adventure, shopping, culture, nature, nightlife");
        return sb.toString();
    }

    private List<ItineraryDay> parseItinerary(String json) {
        try {
            // Clean the response — remove markdown fences if present
            String cleaned = json.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
            }
            return objectMapper.readValue(cleaned, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse Gemini JSON: {}", e.getMessage());
            return null;
        }
    }

    private List<ItineraryDay> fallbackItinerary(String destination, int days) {
        List<ItineraryDay> itinerary = new ArrayList<>();
        for (int d = 1; d <= days; d++) {
            List<ItineraryActivity> activities = List.of(
                new ItineraryActivity("Explore local markets of " + destination,
                    "09:00", "11:00", 500, "shopping"),
                new ItineraryActivity("Visit popular attraction in " + destination,
                    "11:30", "13:30", 300, "sightseeing"),
                new ItineraryActivity("Local cuisine lunch",
                    "14:00", "15:00", 600, "food"),
                new ItineraryActivity("Evening leisure & cultural experience",
                    "16:00", "18:30", 400, "culture")
            );
            itinerary.add(new ItineraryDay(d, activities));
        }
        return itinerary;
    }

    // --- Gemini API response records ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GeminiResponse(List<Candidate> candidates) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Candidate(Content content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Content(List<Part> parts) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Part(String text) {}
}
