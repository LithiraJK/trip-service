package com.tripvisito.tripservice.document.embedded;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** A single day in the AI-generated trip itinerary. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ItineraryDay {

    /** Day number (1-based). */
    private int day;

    /**
     * Location for this day (may differ from trip's main city for multi-destination trips).
     */
    private String location;

    /** Time-ordered list of activities for the day. */
    @Builder.Default
    private List<Activity> activities = new ArrayList<>();
}
