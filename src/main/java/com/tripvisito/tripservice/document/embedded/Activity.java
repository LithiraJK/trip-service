package com.tripvisito.tripservice.document.embedded;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A single scheduled activity within an {@link ItineraryDay}. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Activity {

    /**
     * Time of the activity (e.g. "09:00 AM", "Morning", "After lunch").
     */
    private String time;

    /** Full description of the activity. */
    private String description;
}
