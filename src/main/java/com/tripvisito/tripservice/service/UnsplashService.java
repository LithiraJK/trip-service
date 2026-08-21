package com.tripvisito.tripservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Unsplash API service for fetching trip cover images.
 *
 * <p>Replicates the {@code searchUnsplashImages()} utility from the original
 * {@code trip.controller.ts}. Searches Unsplash using the trip's country and
 * interests as the query, and returns a list of regular-size image URLs.
 *
 * <p>If the Unsplash API is unavailable or returns no results, this service
 * returns an empty list (graceful degradation — the trip is still saved without images).
 */
@Service
public class UnsplashService {

    private static final Logger log = LoggerFactory.getLogger(UnsplashService.class);
    private static final String UNSPLASH_BASE_URL = "https://api.unsplash.com/search/photos";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${unsplash.access-key}")
    private String accessKey;

    @Value("${unsplash.results-per-query:3}")
    private int resultsPerQuery;

    public UnsplashService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Searches Unsplash for images matching the trip's context.
     *
     * @param country     the trip destination country
     * @param interests   the user's travel interests (used as additional query terms)
     * @param travelStyle the trip's travel style (Cultural, Adventure, etc.)
     * @return list of public image URLs (empty list on failure)
     */
    public List<String> searchImages(String country, List<String> interests, String travelStyle) {
        // Build query string: "France Cultural Art History travel"
        String query = buildQuery(country, interests, travelStyle);

        String url = UNSPLASH_BASE_URL
                + "?query=" + encodeQuery(query)
                + "&per_page=" + resultsPerQuery
                + "&orientation=landscape";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Client-ID " + accessKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            log.info("[UnsplashService] Searching images for: {}", query);
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                log.warn("[UnsplashService] Non-OK response: {}", response.getStatusCode());
                return List.of();
            }

            return extractImageUrls(response.getBody());

        } catch (Exception e) {
            log.warn("[UnsplashService] Image search failed (non-fatal): {}", e.getMessage());
            return List.of(); // Graceful fallback — trip still saved without images
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String buildQuery(String country, List<String> interests, String travelStyle) {
        StringBuilder sb = new StringBuilder(country);
        if (travelStyle != null && !travelStyle.isBlank()) {
            sb.append(" ").append(travelStyle);
        }
        if (interests != null && !interests.isEmpty()) {
            sb.append(" ").append(String.join(" ", interests));
        }
        sb.append(" travel");
        return sb.toString();
    }

    private String encodeQuery(String query) {
        return query.replace(" ", "%20");
    }

    /**
     * Extracts the {@code urls.regular} field from each photo in the Unsplash response.
     */
    private List<String> extractImageUrls(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode results = root.path("results");

        List<String> urls = new ArrayList<>();
        if (results.isArray()) {
            for (JsonNode photo : results) {
                JsonNode regularUrl = photo.path("urls").path("regular");
                if (!regularUrl.isMissingNode()) {
                    urls.add(regularUrl.asText());
                }
            }
        }

        log.info("[UnsplashService] Found {} images", urls.size());
        return urls;
    }
}
