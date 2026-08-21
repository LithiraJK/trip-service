package com.tripvisito.tripservice.document.embedded;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** Embedded GPS location for a trip (stored inside {@link TripDetails}). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Location {

    /** City or region name (e.g. "Paris", "Bali"). */
    private String city;

    /**
     * Geographic coordinates as [latitude, longitude].
     * Used by the React frontend to render the trip on a map.
     * Example: {@code [48.8566, 2.3522]} for Paris.
     */
    @Builder.Default
    private List<Double> coordinates = new ArrayList<>();

    /** OpenStreetMap embed URL for the frontend map component. */
    private String openStreetMap;
}
