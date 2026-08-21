package com.tripvisito.tripservice.repository;

import com.tripvisito.tripservice.document.Trip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data MongoDB repository for {@link Trip} documents.
 *
 * <p>Provides standard CRUD (inherited from {@link MongoRepository}) plus:
 * <ul>
 *   <li>Paginated user-trip queries for the "My Trips" frontend page.</li>
 *   <li>Aggregation-based analytics queries for the internal stats endpoint
 *       consumed by user-service's dashboard.</li>
 * </ul>
 */
@Repository
public interface TripRepository extends MongoRepository<Trip, String> {

    // ── User Queries ──────────────────────────────────────────────────────────

    /** Paginated list of all trips for a specific user (newest first). */
    Page<Trip> findByUserId(String userId, Pageable pageable);

    /** All trips for a user (no pagination) — used for simple lookups. */
    List<Trip> findByUserIdOrderByCreatedAtDesc(String userId);

    /** Count of all trips by a specific user. */
    long countByUserId(String userId);

    // ── Stats: Monthly Count ──────────────────────────────────────────────────

    /** Count of trips created in a given date range. */
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // ── Stats: Trend Data (last 7 days) ──────────────────────────────────────

    /**
     * Groups trip creation counts by day for the last N days.
     * Returns a list of BSON Documents each with:
     * - {@code _id}: date string (YYYY-MM-DD)
     * - {@code count}: number of trips created on that day
     */
    @Aggregation(pipeline = {
        "{ '$match': { 'createdAt': { '$gte': ?0 } } }",
        "{ '$group': { '_id': { '$dateToString': { 'format': '%Y-%m-%d', 'date': '$createdAt' } }, 'count': { '$sum': 1 } } }",
        "{ '$sort': { '_id': 1 } }"
    })
    List<org.bson.Document> findTripTrend(LocalDateTime since);

    // ── Stats: Travel Style Breakdown ─────────────────────────────────────────

    /**
     * Groups all trips by their {@code tripDetails.travelStyle} field.
     * Returns documents with {@code style} and {@code count} fields.
     * Mirrors the original {@code getTripsByTravelStyle()} aggregation.
     */
    @Aggregation(pipeline = {
        "{ '$group': { '_id': { '$toLower': '$tripDetails.travelStyle' }, 'count': { '$sum': 1 } } }",
        "{ '$project': { 'style': '$_id', 'count': 1, '_id': 0 } }",
        "{ '$sort': { 'count': -1 } }"
    })
    List<org.bson.Document> findTripsByTravelStyle();

    // ── Public Browse ─────────────────────────────────────────────────────────

    /** All trips ordered by creation date (public browse, newest first). */
    @Query("{}")
    Page<Trip> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
