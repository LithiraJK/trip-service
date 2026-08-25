package com.tripvisito.tripservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripvisito.tripservice.document.Trip;
import com.tripvisito.tripservice.document.embedded.TripDetails;
import com.tripvisito.tripservice.dto.request.GenerateTripRequest;
import com.tripvisito.tripservice.dto.response.PagedResponse;
import com.tripvisito.tripservice.dto.response.TripResponse;
import com.tripvisito.tripservice.dto.response.TripTrendResult;
import com.tripvisito.tripservice.dto.response.TravelStyleBreakdownResult;
import com.tripvisito.tripservice.exception.TripNotFoundException;
import com.tripvisito.tripservice.repository.TripRepository;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Core trip orchestration service.
 *
 * <p>Coordinates between the three external integrations:
 * <ul>
 *   <li>{@link GeminiService} — generates AI itinerary content</li>
 *   <li>{@link UnsplashService} — fetches trip cover images</li>
 *   <li>{@link GcpStorageService} — uploads user-provided images to GCS</li>
 * </ul>
 * and persists everything to MongoDB via {@link TripRepository}.
 *
 * <p>All user-facing CRUD methods enforce trip ownership by comparing the
 * {@code X-User-Id} header (passed as {@code userId}) against the stored
 * {@code trip.userId}. This prevents users from modifying other users' trips.
 */
@Service
public class TripService {

    private static final Logger log = LoggerFactory.getLogger(TripService.class);

    private final TripRepository tripRepository;
    private final GeminiService geminiService;
    private final GcpStorageService gcpStorageService;
    private final UnsplashService unsplashService;
    private final ObjectMapper objectMapper;

    public TripService(TripRepository tripRepository,
                       GeminiService geminiService,
                       GcpStorageService gcpStorageService,
                       UnsplashService unsplashService,
                       ObjectMapper objectMapper) {
        this.tripRepository = tripRepository;
        this.geminiService = geminiService;
        this.gcpStorageService = gcpStorageService;
        this.unsplashService = unsplashService;
        this.objectMapper = objectMapper;
    }

    // ── Generate Trip (AI) ────────────────────────────────────────────────────

    /**
     * Generates a new AI travel plan and saves it to MongoDB.
     *
     * <p>Flow:
     * <ol>
     *   <li>Call Gemini AI → {@link TripDetails}</li>
     *   <li>Fetch Unsplash cover images (non-blocking failure)</li>
     *   <li>Save {@link Trip} document to MongoDB</li>
     *   <li>Return {@link TripResponse}</li>
     * </ol>
     *
     * Mirrors: {@code POST /api/v1/trip/generate} → {@code generateTrip()}
     */
    public TripResponse generateTrip(GenerateTripRequest request, String userId) {
        log.info("[TripService] Generating trip for userId={}", userId);

        // 1. AI generation (may take 10–60 seconds)
        TripDetails tripDetails = geminiService.generateTripDetails(request);

        // 2. Fetch cover images from Unsplash (graceful degradation on failure)
        List<String> imageUrls = unsplashService.searchImages(
                request.getCountry(),
                request.getInterests(),
                request.getTravelStyle()
        );

        // 3. Save to MongoDB
        Trip trip = Trip.builder()
                .tripDetails(tripDetails)
                .imageUrls(imageUrls)
                .paymentLink("")
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .build();

        Trip saved = tripRepository.save(trip);
        log.info("[TripService] Trip saved: id={}", saved.getId());

        return TripResponse.from(saved);
    }

    // ── Get All Trips (Public Browse) ─────────────────────────────────────────

    /**
     * Returns all trips paginated (public endpoint — no auth required).
     * Mirrors: {@code GET /api/v1/trip/all}
     */
    public PagedResponse<TripResponse> getAllTrips(int page, int limit) {
        PageRequest pageRequest = PageRequest.of(page - 1, limit,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Trip> tripPage = tripRepository.findAll(pageRequest);

        List<TripResponse> items = tripPage.getContent().stream()
                .map(TripResponse::from)
                .toList();

        return PagedResponse.of(items, tripPage.getTotalElements(), page, limit);
    }

    // ── Get Trip by ID ────────────────────────────────────────────────────────

    /**
     * Returns a single trip by its MongoDB ID (public).
     * Mirrors: {@code GET /api/v1/trip/:id}
     */
    public TripResponse getTripById(String tripId) {
        return TripResponse.from(findOrThrow(tripId));
    }

    // ── Get User's Trips ──────────────────────────────────────────────────────

    /**
     * Returns all trips created by a specific user (paginated).
     * Mirrors: {@code GET /api/v1/trip/user-trips}
     */
    public PagedResponse<TripResponse> getUserTrips(String userId, int page, int limit) {
        PageRequest pageRequest = PageRequest.of(page - 1, limit,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Trip> tripPage = tripRepository.findByUserId(userId, pageRequest);

        List<TripResponse> items = tripPage.getContent().stream()
                .map(TripResponse::from)
                .toList();

        return PagedResponse.of(items, tripPage.getTotalElements(), page, limit);
    }

    // ── Update Trip ───────────────────────────────────────────────────────────

    /**
     * Updates a trip's details and/or images.
     *
     * <p>Accepts mixed multipart/form-data:
     * <ul>
     *   <li>{@code tripDetailsJson} — updated TripDetails as JSON string</li>
     *   <li>{@code existingImagesJson} — JSON array of existing image URLs to keep</li>
     *   <li>{@code newImageFiles} — new image files to upload to GCP Storage</li>
     * </ul>
     *
     * Mirrors: {@code PUT /api/v1/trip/edit/:tripId}
     */
    public TripResponse updateTrip(String tripId,
                                   String userId,
                                   String tripDetailsJson,
                                   String existingImagesJson,
                                   List<MultipartFile> newImageFiles) throws IOException {
        Trip trip = findOrThrow(tripId);

        // ── Ownership check bypassed ────────────────────────────────────────
        log.info("[TripService] User {} updating trip owned by {}", userId, trip.getUserId());

        // ── Parse updated trip details ─────────────────────────────────────
        if (tripDetailsJson != null && !tripDetailsJson.isBlank()) {
            TripDetails updatedDetails = objectMapper.readValue(tripDetailsJson, TripDetails.class);
            trip.setTripDetails(updatedDetails);
        }

        // ── Build final image list ─────────────────────────────────────────
        List<String> imageUrls = new ArrayList<>();

        // Add existing images the user wants to keep
        if (existingImagesJson != null && !existingImagesJson.isBlank()) {
            String[] existing = objectMapper.readValue(existingImagesJson, String[].class);
            imageUrls.addAll(Arrays.asList(existing));
        }

        // Upload new images to GCP Storage
        if (newImageFiles != null) {
            for (MultipartFile file : newImageFiles) {
                if (!file.isEmpty()) {
                    String gcsUrl = gcpStorageService.uploadFile(
                            file.getOriginalFilename(),
                            file.getBytes(),
                            file.getContentType()
                    );
                    imageUrls.add(gcsUrl);
                    log.info("[TripService] Uploaded new image to GCS: {}", gcsUrl);
                }
            }
        }

        if (!imageUrls.isEmpty()) {
            trip.setImageUrls(imageUrls);
        }

        Trip saved = tripRepository.save(trip);
        return TripResponse.from(saved);
    }

    // ── Delete Trip ───────────────────────────────────────────────────────────

    /**
     * Deletes a trip. Only the owner can delete their trip.
     * Mirrors: {@code DELETE /api/v1/trip/delete/:id}
     */
    public void deleteTrip(String tripId, String userId) {
        Trip trip = findOrThrow(tripId);

        // ── Ownership check bypassed ────────────────────────────────────────
        log.info("[TripService] User {} deleting trip owned by {}", userId, trip.getUserId());

        tripRepository.delete(trip);
        log.info("[TripService] Trip deleted: id={}", tripId);
    }

    // ── Internal Stats (for user-service dashboard) ───────────────────────────

    /**
     * Returns aggregated trip stats for the admin dashboard.
     * Called by user-service via Feign client on {@code GET /internal/stats}.
     */
    public Map<String, Object> getInternalStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(7);
        LocalDateTime startOfCurrentMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime startOfLastMonth = startOfCurrentMonth.minusMonths(1);

        long total = tripRepository.count();
        long currentMonth = tripRepository.countByCreatedAtBetween(startOfCurrentMonth, now);
        long lastMonth = tripRepository.countByCreatedAtBetween(startOfLastMonth, startOfCurrentMonth);

        // Daily trend for last 7 days
        List<Integer> trend = tripRepository.findTripTrend(sevenDaysAgo)
                .stream()
                .map(item -> item.getCount() != null ? item.getCount() : 0)
                .collect(Collectors.toList());

        // Travel style breakdown
        List<Map<String, Object>> travelStyleBreakdown = tripRepository.findTripsByTravelStyle()
                .stream()
                .map(item -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("style", item.getStyle());
                    entry.put("count", item.getCount() != null ? item.getCount().longValue() : 0L);
                    return entry;
                })
                .collect(Collectors.toList());

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("currentMonth", currentMonth);
        stats.put("lastMonth", lastMonth);
        stats.put("trend", trend);
        stats.put("travelStyleBreakdown", travelStyleBreakdown);

        return stats;
    }

    // ── Batch Retrieval (Internal) ───────────────────────────────────────────

    /**
     * Returns a list of TripResponses for the given list of IDs.
     */
    public List<TripResponse> getTripsByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return tripRepository.findAllById(ids).stream()
                .map(TripResponse::from)
                .toList();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Trip findOrThrow(String tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException("Trip not found: " + tripId));
    }
}

