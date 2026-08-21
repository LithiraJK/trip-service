package com.tripvisito.tripservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Generic paginated response — same shape as user-service. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {

    private List<T> items;
    private int totalPages;
    private long totalCount;
    private int page;

    public static <T> PagedResponse<T> of(List<T> items, long totalCount, int page, int limit) {
        return PagedResponse.<T>builder()
                .items(items)
                .totalCount(totalCount)
                .totalPages((int) Math.ceil((double) totalCount / limit))
                .page(page)
                .build();
    }
}
