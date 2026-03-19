package com.Rothana.hotel_booking_system.features.Payments.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Integer id,
        Integer bookingId,
        PaymentUserDTO user,
        String provider,
        String status,
        BigDecimal amount,
        String currency,
        LocalDateTime createdAt
) {
}
