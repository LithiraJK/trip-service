package com.tripvisito.tripservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

/**
 * Request body for {@code POST /api/v1/trip/generate}.
 *
 * <p>These fields are used to:
 * <ol>
 *   <li>Build the Google Gemini prompt via {@link com.tripvisito.tripservice.service.GeminiService}</li>
 *   <li>Query Unsplash for trip cover images via {@link com.tripvisito.tripservice.service.UnsplashService}</li>
 * </ol>
 *
 * <p>Mirrors the original {@code trip.controller.ts} → {@code generateTrip()} req.body fields.
 */
@Data
public class GenerateTripRequest {

    @NotBlank(message = "Country is required")
    private String country;

    @NotBlank(message = "Number of days is required")
    @com.fasterxml.jackson.annotation.JsonAlias("duration")
    private String numberOfDays;

    @NotBlank(message = "Budget is required")
    private String budget;

    @NotEmpty(message = "At least one interest is required")
    @com.fasterxml.jackson.annotation.JsonFormat(with = com.fasterxml.jackson.annotation.JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> interests;

    @NotBlank(message = "Travel style is required")
    private String travelStyle;

    @NotBlank(message = "Group type is required")
    private String groupType;

    /**
     * Maximum number of tokens for the Gemini response.
     * Defaults to 8000 to ensure a complete itinerary is generated.
     */
    @Min(value = 1000, message = "maxToken must be at least 1000")
    @Max(value = 16000, message = "maxToken must not exceed 16000")
    private int maxToken = 8000;
}
