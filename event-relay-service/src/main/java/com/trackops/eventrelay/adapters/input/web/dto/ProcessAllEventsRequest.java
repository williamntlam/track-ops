package com.trackops.eventrelay.adapters.input.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessAllEventsRequest {
    
    @Min(value = 1, message = "Batch size must be at least 1")
    @Max(value = 100, message = "Batch size cannot exceed 100")
    private int batchSize = 10;
    
    private boolean forceRetry = false;
}
