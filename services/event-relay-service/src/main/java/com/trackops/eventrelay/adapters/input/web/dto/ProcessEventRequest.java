package com.trackops.eventrelay.adapters.input.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessEventRequest {
    
    @NotNull(message = "Event ID is required")
    private UUID eventId;
    
    private boolean forceRetry = false;
}
