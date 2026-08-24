package com.tripvisito.tripservice.controller;

import com.tripvisito.tripservice.dto.request.GenerateTripRequest;
import com.tripvisito.tripservice.dto.response.ApiResponse;
import com.tripvisito.tripservice.dto.response.PagedResponse;
import com.tripvisito.tripservice.dto.response.TripResponse;
import com.tripvisito.tripservice.service.TripService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * REST controller for all trip endpoints.
 *
 * <p>Maps the original Express {@code trip.routes.ts} to Spring MVC.
 * The {@code X-User-Id} header is injected by the api-gateway JWT filter
 * and used for ownership verification instead of decoding a JWT directly.
 *
 * <p>Public routes ({@code GET /all}, {@code GET /:id}) are accessible without
 * authentication (the gateway allows GET-only public access for these paths).
 * All write operations require the gateway to have validated the user's JWT.
 */
@RestController
@RequestMapping({"/api/v1/trip", "/trip", "/trips", "/api/trips"})
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    // ── AI Generation ─────────────────────────────────────────────────────────

    /**
     * POST /api/v1/trip/generate — Authenticated
     * Generates a new AI travel plan using Google Gemini.
     */
    @PostMapping({"/generate", "/generate-trip"})
    public ResponseEntity<ApiResponse<TripResponse>> generateTrip(
            @Valid @RequestBody GenerateTripRequest request,
            @RequestHeader("X-User-Id") String userId) {
        TripResponse trip = tripService.generateTrip(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(201, "Trip generated successfully", trip));
    }

    // ── Public Browse ─────────────────────────────────────────────────────────

    /**
     * GET /api/v1/trip/all?page=1&limit=4 — Public
     * Returns paginated list of all trips (newest first).
     */
    @GetMapping({"/all", ""})
    public ResponseEntity<ApiResponse<PagedResponse<TripResponse>>> getAllTrips(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "4") int limit) {
        PagedResponse<TripResponse> result = tripService.getAllTrips(page, limit);
        return ResponseEntity.ok(ApiResponse.success("Trips fetched successfully", result));
    }

    /**
     * GET /api/v1/trip/{id} — Public
     * Returns a single trip by its MongoDB ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TripResponse>> getTripById(@PathVariable String id) {
        TripResponse trip = tripService.getTripById(id);
        return ResponseEntity.ok(ApiResponse.success("Trip retrieved successfully", trip));
    }

    // ── Authenticated: User-Specific ──────────────────────────────────────────

    /**
     * GET /api/v1/trip/user-trips?page=1&limit=4 — Authenticated
     * Returns the authenticated user's trips (paginated).
     */
    @GetMapping("/user-trips")
    public ResponseEntity<ApiResponse<PagedResponse<TripResponse>>> getUserTrips(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "4") int limit) {
        PagedResponse<TripResponse> result = tripService.getUserTrips(userId, page, limit);
        return ResponseEntity.ok(ApiResponse.success("User trips fetched successfully", result));
    }

    // ── Authenticated: CRUD ───────────────────────────────────────────────────

    /**
     * PUT /api/v1/trip/edit/{tripId} — Authenticated, Owner only
     *
     * <p>Accepts multipart/form-data with:
     * <ul>
     *   <li>{@code tripDetails} — updated TripDetails as JSON string</li>
     *   <li>{@code existingImages} — JSON array of URLs to keep</li>
     *   <li>{@code imageURLs} — new image files to upload to GCP Storage</li>
     * </ul>
     */
    @PutMapping(value = "/edit/{tripId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TripResponse>> updateTrip(
            @PathVariable String tripId,
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(value = "tripDetails", required = false) String tripDetailsJson,
            @RequestParam(value = "existingImages", required = false) String existingImagesJson,
            @RequestParam(value = "imageURLs", required = false) List<MultipartFile> newImages)
            throws IOException {
        TripResponse updated = tripService.updateTrip(
                tripId, userId, tripDetailsJson, existingImagesJson, newImages);
        return ResponseEntity.ok(ApiResponse.success("Trip updated successfully", updated));
    }

    /**
     * DELETE /api/v1/trip/delete/{id} — Authenticated, Owner only
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTrip(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId) {
        tripService.deleteTrip(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Trip deleted successfully"));
    }
}
