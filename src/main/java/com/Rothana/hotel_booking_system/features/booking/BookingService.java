package com.Rothana.hotel_booking_system.features.booking;

import com.Rothana.hotel_booking_system.features.booking.dto.BookingCreateRequest;
import com.Rothana.hotel_booking_system.features.booking.dto.BookingResponse;
import com.Rothana.hotel_booking_system.features.booking.dto.BookingUpdateRequest;

import java.util.List;

public interface BookingService {

    BookingResponse create(BookingCreateRequest request);
    BookingResponse update(Integer id ,BookingUpdateRequest request);
    BookingResponse findById(Integer id);
    List<BookingResponse> findAll();
    void delete(Integer id);
    List<BookingResponse> findBookingsByUserId(Integer userId);
    BookingResponse payBooking(Integer bookingId);


}
