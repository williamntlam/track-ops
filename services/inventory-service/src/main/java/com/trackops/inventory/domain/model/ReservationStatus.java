package com.trackops.inventory.domain.model;

public enum ReservationStatus {
    PENDING,    // Reservation request received
    RESERVED,   // Inventory successfully reserved
    RELEASED,   // Reservation was released
    EXPIRED     // Reservation expired
}
