package com.Rothana.hotel_booking_system.features.Payments;

import com.Rothana.hotel_booking_system.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    Optional<Payment> findByPaypalOrderId(String paypalOrderId);
    boolean existsByPaypalOrderId(String paypalOrderId);

    Optional<Payment> findByBookingId(Integer bookingId);


}
