package com.tripvisito.tripservice.controller;

import com.tripvisito.tripservice.dto.request.GenerateTripRequest;
import com.tripvisito.tripservice.dto.request.TripRequest;
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

@RestController
@RequestMapping({"/api/v1/trip", "/trip", "/trips", "/api/trips"})
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    // POST /api/v1/trip/generate — Authenticated / Public
    @PostMapping({"/generate", "/generate-trip"})
    public ResponseEntity<ApiResponse<TripResponse>> generateTrip(
            @Valid @RequestBody GenerateTripRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        String finalUserId = userId != null ? userId : "3";
        TripResponse trip = tripService.generateTrip(request, finalUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(201, "Trip generated successfully", trip));
    }

    // GET /api/v1/trip/all — Public
    @GetMapping({"/all", ""})
    public ResponseEntity<ApiResponse<PagedResponse<TripResponse>>> getAllTrips(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "4") int limit) {
        PagedResponse<TripResponse> result = tripService.getAllTrips(page, limit);
        return ResponseEntity.ok(ApiResponse.success("Trips fetched successfully", result));
    }

    // GET /api/v1/trip/{id} — Public
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TripResponse>> getTripById(@PathVariable String id) {
        TripResponse trip = tripService.getTripById(id);
        return ResponseEntity.ok(ApiResponse.success("Trip retrieved successfully", trip));
    }

    // GET /api/v1/trip/user-trips — Authenticated / Public
    @GetMapping("/user-trips")
    public ResponseEntity<ApiResponse<PagedResponse<TripResponse>>> getUserTrips(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "4") int limit) {
        String finalUserId = userId != null ? userId : "3";
        PagedResponse<TripResponse> result = tripService.getUserTrips(finalUserId, page, limit);
        return ResponseEntity.ok(ApiResponse.success("User trips fetched successfully", result));
    }

    // PUT /api/v1/trip/edit/{tripId} — Authenticated / Public
    @PutMapping(value = "/edit/{tripId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TripResponse>> updateTrip(
            @PathVariable String tripId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestParam(value = "tripDetails", required = false) String tripDetailsJson,
            @RequestParam(value = "existingImages", required = false) String existingImagesJson,
            @RequestParam(value = "imageURLs", required = false) List<MultipartFile> newImages)
            throws IOException {
        String finalUserId = userId != null ? userId : "3";
        TripResponse updated = tripService.updateTrip(
                tripId, finalUserId, tripDetailsJson, existingImagesJson, newImages);
        return ResponseEntity.ok(ApiResponse.success("Trip updated successfully", updated));
    }

    // DELETE /api/v1/trip/delete/{id} — Authenticated / Public
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTrip(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        String finalUserId = userId != null ? userId : "3";
        tripService.deleteTrip(id, finalUserId);
        return ResponseEntity.ok(ApiResponse.success("Trip deleted successfully"));
    }

    // POST /api/v1/trip — Public / Admin
    @PostMapping({"", "/"})
    public ResponseEntity<ApiResponse<TripResponse>> createTrip(
            @Valid @RequestBody TripRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        String finalUserId = userId != null ? userId : "1";
        TripResponse trip = tripService.createTrip(request, finalUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "Trip created successfully", trip));
    }

    // PUT /api/v1/trip/{id} — Public / Admin
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TripResponse>> updateTripDirect(
            @PathVariable String id,
            @Valid @RequestBody TripRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        String finalUserId = userId != null ? userId : "1";
        TripResponse updated = tripService.updateTripDirect(id, request, finalUserId);
        return ResponseEntity.ok(ApiResponse.success("Trip updated successfully", updated));
    }

    // DELETE /api/v1/trip/{id} — Public / Admin
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTripDirect(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        String finalUserId = userId != null ? userId : "1";
        tripService.deleteTrip(id, finalUserId);
        return ResponseEntity.ok(ApiResponse.success("Trip deleted successfully"));
    }
}
