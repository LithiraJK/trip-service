package com.tripvisito.tripservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravelStyleBreakdownResult {
    private String style;
    private Integer count;
}
