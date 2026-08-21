package com.tripvisito.tripservice.controller;

import com.tripvisito.tripservice.dto.response.ApiResponse;
import com.tripvisito.tripservice.service.TripService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tripvisito.tripservice.dto.response.TripResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/**
 * Internal-only controller for cross-service communication.
 *
 * <p>Exposes {@code GET /internal/stats} which is called by {@code user-service}'s
 * {@link com.tripvisito.userservice.client.TripServiceClient} Feign client
 * to aggregate trip statistics for the admin dashboard.
 *
 * <p><b>Security note:</b> This endpoint is <b>NOT routed</b> through the api-gateway
 * (no {@code /api/v1/trip/internal/**} route is defined in the gateway config).
 * It is only accessible within the GCP VPC private network via service-to-service
 * Eureka-resolved calls. External clients cannot reach it through the public gateway.
 */
@RestController
@RequestMapping("/internal")
public class InternalController {

    private final TripService tripService;

    public InternalController(TripService tripService) {
        this.tripService = tripService;
    }

    /**
     * GET /internal/stats
     * Returns trip aggregate stats for the user-service dashboard.
     *
     * <p>Response shape:
     * <pre>
     * {
     *   "status": 200,
     *   "data": {
     *     "total": 120,
     *     "currentMonth": 15,
     *     "lastMonth": 20,
     *     "trend": [2, 3, 1, 4, 2, 3, 0],
     *     "travelStyleBreakdown": [
     *       { "style": "cultural", "count": 45 },
     *       { "style": "adventure", "count": 30 }
     *     ]
     *   }
     * }
     * </pre>
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInternalStats() {
        Map<String, Object> stats = tripService.getInternalStats();
        return ResponseEntity.ok(ApiResponse.success("Trip stats", stats));
    }

    /**
     * POST /internal/trips
     * Returns a list of trip details by their IDs for internal services.
     */
    @PostMapping("/trips")
    public ResponseEntity<ApiResponse<List<TripResponse>>> getTripsByIds(@RequestBody List<String> ids) {
        List<TripResponse> trips = tripService.getTripsByIds(ids);
        return ResponseEntity.ok(ApiResponse.success("Trips fetched successfully", trips));
    }
}
