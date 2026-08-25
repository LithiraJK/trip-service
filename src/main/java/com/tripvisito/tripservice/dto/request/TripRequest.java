package com.tripvisito.tripservice.dto.request;

import com.tripvisito.tripservice.document.embedded.TripDetails;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripRequest {
    private TripDetails tripDetails;
    private List<String> imageUrls;
    private String userId;
}
