package com.tripvisito.tripservice.document;

import com.tripvisito.tripservice.document.embedded.TripDetails;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB document representing a Tripvisito travel plan.
 *
 * <p>Migrated from the Mongoose {@code trip.model.ts} schema. Key design differences
 * from the original:
 *
 * <ul>
 *   <li><b>tripDetails</b> was stored as a JSON string in the original. It is now
 *       a fully structured nested document ({@link TripDetails}), enabling proper
 *       MongoDB field-level querying and aggregation (e.g., groupBy travelStyle)
 *       without runtime JSON parsing.</li>
 *   <li><b>imageUrls</b> now contains GCP Cloud Storage URLs (instead of Cloudinary)
 *       for images uploaded via the update endpoint.</li>
 *   <li><b>userId</b> is a String referencing the MySQL user ID from user-service.
 *       No cross-database FK constraint is possible — ownership is enforced at the
 *       service layer by comparing against the {@code X-User-Id} gateway header.</li>
 * </ul>
 *
 * <p>Stored in the {@code tripvisito_trips} MongoDB database,
 * {@code trips} collection.
 */
@Document(collection = "trips")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trip {

    @Id
    private String id;

    /**
     * Structured AI-generated trip content.
     * Contains name, description, itinerary, location, budget, weather info, etc.
     * Stored as a nested MongoDB document (not a string).
     */
    private TripDetails tripDetails;

    /**
     * Public URLs of trip cover images.
     * Sources: Unsplash (auto-fetched on generation), GCP Storage (user-uploaded).
     */
    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();

    /**
     * Stripe payment link for booking this trip (set by payment-service).
     * Empty string until a checkout session is created.
     */
    @Builder.Default
    private String paymentLink = "";

    /**
     * MySQL user ID (from user-service) who created this trip.
     * Indexed for fast user-trips queries.
     */
    @Indexed
    private String userId;

    /**
     * Explicit creation timestamp used for analytics (monthly trip counts, trend charts).
     * Separate from the auto-managed {@code createdAt} so that the stat aggregations
     * match the original Mongoose {@code createdAt} field semantics.
     */
    private LocalDateTime createdAt;

    @CreatedDate
    private LocalDateTime mongoCreatedAt;

    @LastModifiedDate
    private LocalDateTime mongoUpdatedAt;
}
