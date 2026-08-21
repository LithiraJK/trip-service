package com.tripvisito.tripservice.dto.response;

import com.tripvisito.tripservice.document.Trip;
import com.tripvisito.tripservice.document.embedded.TripDetails;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Full trip response DTO.
 * Returned on generate, get-by-id, and update operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripResponse {

    private String id;
    private TripDetails tripDetails;
    private List<String> imageUrls;
    private String paymentLink;
    private String userId;
    private LocalDateTime createdAt;

    public static TripResponse from(Trip trip) {
        return TripResponse.builder()
                .id(trip.getId())
                .tripDetails(trip.getTripDetails())
                .imageUrls(trip.getImageUrls())
                .paymentLink(trip.getPaymentLink())
                .userId(trip.getUserId())
                .createdAt(trip.getCreatedAt())
                .build();
    }
}
