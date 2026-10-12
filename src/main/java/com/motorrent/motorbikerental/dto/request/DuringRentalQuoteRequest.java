package com.motorrent.motorbikerental.dto.request;

import java.time.LocalDateTime;

/** Staff arranges the exchange; rental charges come from the catalog snapshot. */
public record DuringRentalQuoteRequest(
        Long motorbikeId,
        LocalDateTime plannedChangeTime,
        LocalDateTime newReturnTime,
        Integer vehicleChangeFee,
        String exchangeAddress,
        String reason) {}
