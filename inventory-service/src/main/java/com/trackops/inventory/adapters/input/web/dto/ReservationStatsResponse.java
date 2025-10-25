package com.trackops.inventory.adapters.input.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationStatsResponse {
    
    private long totalReservations;
    private long activeReservations;
    private long expiredReservations;
    private long releasedReservations;
}
