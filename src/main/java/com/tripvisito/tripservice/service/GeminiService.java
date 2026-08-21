package com.tripvisito.tripservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripvisito.tripservice.document.embedded.TripDetails;
import com.tripvisito.tripservice.dto.request.GenerateTripRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Google Gemini AI service for generating personalised travel itineraries.
 *
 * <p>Replicates the {@code generateTrip()} AI call from the original
 * {@code trip.controller.ts}, translated to a Spring service with proper
 * error handling and response parsing.
 *
 * <h3>API Used</h3>
 * <p>Gemini 2.5 Flash via the REST API:
 * {@code POST https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent}
 *
 * <h3>Prompt Design</h3>
 * <p>The prompt instructs Gemini to return a strict JSON object that maps
 * exactly to {@link TripDetails}. Key fields: name, description, estimatedPrice,
 * duration, budget, travelStyle, country, interests, groupType, bestTimeToVisit,
 * weatherInfo, location, itinerary.
 *
 * <h3>Parsing Strategy</h3>
 * <p>Gemini often wraps JSON in markdown code fences. This service tries:
 * <ol>
 *   <li>Direct JSON parse of the raw text</li>
 *   <li>Extract content from ````json ... ```` fences</li>
 * </ol>
 */
@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Value("${gemini.api.max-output-tokens:8000}")
    private int defaultMaxOutputTokens;

    public GeminiService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Calls Google Gemini to generate a complete travel itinerary.
     *
     * @param request the user's trip preferences
     * @return parsed {@link TripDetails} object populated with AI-generated content
     * @throws RuntimeException if the AI call or JSON parsing fails
     */
    public TripDetails generateTripDetails(GenerateTripRequest request) {
        String prompt = buildPrompt(request);

        // ── Build Gemini request body ─────────────────────────────────────
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", Map.of(
                        "maxOutputTokens", request.getMaxToken() > 0
                                ? request.getMaxToken() : defaultMaxOutputTokens
                )
        );

        // ── Build HTTP headers ────────────────────────────────────────────
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        log.info("[GeminiService] Generating trip for: country={}, style={}, days={}",
                request.getCountry(), request.getTravelStyle(), request.getNumberOfDays());

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new RuntimeException("Gemini API returned status: " + response.getStatusCode());
            }

            // ── Extract text from Gemini response ─────────────────────────
            String rawText = extractTextFromResponse(response.getBody());
            log.debug("[GeminiService] Raw Gemini text (first 200 chars): {}",
                    rawText.substring(0, Math.min(200, rawText.length())));

            // ── Parse JSON → TripDetails ──────────────────────────────────
            return parseToTripDetails(rawText);

        } catch (Exception e) {
            log.error("[GeminiService] Trip generation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate trip with AI: " + e.getMessage(), e);
        }
    }

    // ── Prompt Builder ────────────────────────────────────────────────────────

    /**
     * Constructs the Gemini prompt that requests a structured JSON response.
     * Mirrors the exact prompt structure from the original {@code trip.controller.ts}.
     */
    private String buildPrompt(GenerateTripRequest r) {
        return String.format("""
                Generate a travel plan for the following details and return ONLY a valid JSON object
                with no markdown, no code fences, no explanation — just the raw JSON.
                
                Travel Details:
                - Country: %s
                - Number of Days: %s
                - Budget: %s
                - Interests: %s
                - Travel Style: %s
                - Group Type: %s
                
                Return a JSON object with EXACTLY this structure:
                {
                  "name": "string — creative trip name",
                  "description": "string — 2–3 sentence engaging description",
                  "estimatedPrice": "string — estimated total cost in USD (e.g. '1200')",
                  "duration": number (days),
                  "budget": "string — budget tier (e.g. Moderate, Budget, Luxury)",
                  "travelStyle": "string",
                  "country": "string",
                  "interests": ["string"],
                  "groupType": "string",
                  "bestTimeToVisit": ["string — with emoji"],
                  "weatherInfo": ["string — with emoji"],
                  "location": {
                    "city": "string — primary city",
                    "coordinates": [latitude_number, longitude_number],
                    "openStreetMap": "string — OpenStreetMap embed URL"
                  },
                  "itinerary": [
                    {
                      "day": 1,
                      "location": "string",
                      "activities": [
                        { "time": "string", "description": "string" }
                      ]
                    }
                  ]
                }
                """,
                r.getCountry(),
                r.getNumberOfDays(),
                r.getBudget(),
                String.join(", ", r.getInterests()),
                r.getTravelStyle(),
                r.getGroupType()
        );
    }

    // ── Response Parsing ──────────────────────────────────────────────────────

    /**
     * Extracts the text content from the Gemini response JSON.
     * Handles both direct text and nested content.parts[0].text structures.
     */
    private String extractTextFromResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);

        // Path 1: candidates[0].content.parts[0].text (standard)
        JsonNode text = root.path("candidates").path(0)
                .path("content").path("parts").path(0).path("text");

        if (!text.isMissingNode() && !text.isNull()) {
            return text.asText();
        }

        // Path 2: candidates[0].content[0].text (alternative structure)
        text = root.path("candidates").path(0)
                .path("content").path(0).path("text");

        if (!text.isMissingNode() && !text.isNull()) {
            return text.asText();
        }

        throw new RuntimeException("Could not extract text from Gemini response: " + responseBody);
    }

    /**
     * Parses raw Gemini text into a {@link TripDetails} object.
     * Handles JSON wrapped in markdown code fences (```json ... ```).
     */
    private TripDetails parseToTripDetails(String rawText) throws Exception {
        String json = rawText.trim();

        // Try direct parse first
        try {
            return objectMapper.readValue(json, TripDetails.class);
        } catch (Exception e) {
            log.debug("[GeminiService] Direct JSON parse failed, trying to strip markdown fences...");
        }

        // Strip markdown code fences: ```json ... ``` or ``` ... ```
        if (json.contains("```")) {
            int startIdx = json.indexOf("```");
            int endIdx = json.lastIndexOf("```");
            if (startIdx != endIdx) {
                // Move past the opening fence and optional "json" lang tag
                int contentStart = json.indexOf('\n', startIdx) + 1;
                String stripped = json.substring(contentStart, endIdx).trim();
                return objectMapper.readValue(stripped, TripDetails.class);
            }
        }

        throw new RuntimeException("Could not parse Gemini response as TripDetails JSON");
    }
}
