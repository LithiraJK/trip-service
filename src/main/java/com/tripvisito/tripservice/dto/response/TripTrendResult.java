package com.tripvisito.tripservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripTrendResult {
    private String id; // Spring Data Maps _id to id automatically
    private Integer count;
}
