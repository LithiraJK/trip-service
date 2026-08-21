package com.tripvisito.tripservice.document.embedded;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Embedded MongoDB document containing the full AI-generated trip content.
 *
 * <p>This maps directly to the JSON structure produced by the Google Gemini
 * prompt defined in {@link com.tripvisito.tripservice.service.GeminiService}.
 *
 * <p>{@code @JsonIgnoreProperties(ignoreUnknown = true)} allows graceful handling
 * of any extra fields Gemini might add to its response without breaking deserialization.
 *
 * <p>Example JSON shape (produced by Gemini):
 * <pre>
 * {
 *   "name": "7-Day Paris Adventure",
 *   "description": "...",
 *   "estimatedPrice": "1200",
 *   "duration": 7,
 *   "budget": "Moderate",
 *   "travelStyle": "Cultural",
 *   "country": "France",
 *   "interests": ["Art", "History"],
 *   "groupType": "Couple",
 *   "bestTimeToVisit": ["🌸 Spring (Apr–Jun): Perfect weather"],
 *   "weatherInfo": ["🌸 Spring: 15–22°C"],
 *   "location": { "city": "Paris", "coordinates": [48.8566, 2.3522], "openStreetMap": "..." },
 *   "itinerary": [{ "day": 1, "location": "Paris", "activities": [...] }]
 * }
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TripDetails {

    private String name;
    private String description;
    private String estimatedPrice;
    private int duration;
    private String budget;
    private String travelStyle;
    private String country;

    @Builder.Default
    private List<String> interests = new ArrayList<>();

    private String groupType;

    @Builder.Default
    private List<String> bestTimeToVisit = new ArrayList<>();

    @Builder.Default
    private List<String> weatherInfo = new ArrayList<>();

    private Location location;

    @Builder.Default
    private List<ItineraryDay> itinerary = new ArrayList<>();
}
