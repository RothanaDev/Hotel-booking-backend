package com.Rothana.hotel_booking_system.features.booking;

import com.Rothana.hotel_booking_system.features.booking.dto.BookingCreateRequest;
import com.Rothana.hotel_booking_system.features.booking.dto.BookingResponse;
import com.Rothana.hotel_booking_system.features.booking.dto.BookingUpdateRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@AllArgsConstructor
public class BookingController {
    private  final BookingService bookingService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/create")
    public BookingResponse create(@RequestBody BookingCreateRequest request){
        return bookingService.create(request);
    }

    @PutMapping("/{id}")
    public BookingResponse update(@PathVariable("id") Integer id, @RequestBody BookingUpdateRequest request){
        return bookingService.update(id,request);
    }

    @GetMapping("/{id}")
    public BookingResponse findById(@PathVariable Integer id){
        return bookingService.findById(id);
    }
    @GetMapping()
    public List<BookingResponse> findAll(){
        return bookingService.findAll();
    }
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Integer id){
        bookingService.delete(id);
    }
    @GetMapping("/user/{userId}")
    public List<BookingResponse> getBookingsByUser(@PathVariable Integer userId) {
        return bookingService.findBookingsByUserId(userId);
    }
    @PutMapping("/{id}/pay")
    public BookingResponse payBooking(@PathVariable Integer id){
        return bookingService.payBooking(id);
    }

}
