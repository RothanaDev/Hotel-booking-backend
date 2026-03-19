package com.Rothana.hotel_booking_system.features.booking;

import com.Rothana.hotel_booking_system.entity.Booking;
import com.Rothana.hotel_booking_system.entity.BookingService; // ✅ ENTITY (join table)
import com.Rothana.hotel_booking_system.entity.HotelService;
import com.Rothana.hotel_booking_system.entity.Room;
import com.Rothana.hotel_booking_system.entity.User;
import com.Rothana.hotel_booking_system.features.ServiceBooking.ServiceBookingRepository;
import com.Rothana.hotel_booking_system.features.telegram.TelegramNotifyService;
import com.Rothana.hotel_booking_system.features.booking.dto.BookingCreateRequest;
import com.Rothana.hotel_booking_system.features.booking.dto.BookingResponse;
import com.Rothana.hotel_booking_system.features.booking.dto.BookingUpdateRequest;
import com.Rothana.hotel_booking_system.features.room.RoomRepository;
import com.Rothana.hotel_booking_system.features.service.ServiceRepository;
import com.Rothana.hotel_booking_system.features.user.UserRepository;
import com.Rothana.hotel_booking_system.mapper.BookingMapper;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@AllArgsConstructor
public class BookingServiceImpl implements com.Rothana.hotel_booking_system.features.booking.BookingService {

        private final BookingMapper bookingMapper;
        private final BookingRepository bookingRepository;
        private final RoomRepository roomRepository;
        private final UserRepository userRepository;

        private final ServiceRepository serviceRepository;
        private final ServiceBookingRepository serviceBookingRepository;
        private final TelegramNotifyService telegramNotifyService;

        @Transactional
        @Override
        public BookingResponse create(BookingCreateRequest request) {

                // 1) Validate dates
                if (request.checkout().isBefore(request.checkin()) ||
                                request.checkout().isEqual(request.checkin())) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Check-out date must be after check-in date");
                }

                // 2) Find Room
                Room room = roomRepository.findById(request.roomId())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Room Not Found"));

                if (!"available".equalsIgnoreCase(room.getStatus())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room is not available");
                }
                room.setStatus("booked");
                roomRepository.save(room);

                // 3) Find User
                User user = userRepository.findById(request.userId())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "User Not Found"));

                // 4) Create Booking entity
                Booking booking = bookingMapper.fromBookingCreateRequest(request);
                booking.setRoom(room);
                booking.setUser(user);

                booking.setTotalNumOfGuest(request.numOfAdults() + request.numOfChildren());
                booking.setStatus("pending");
                booking.setCreatedAt(java.time.LocalDateTime.now());

                // 5) Calculate room total
                long nights = ChronoUnit.DAYS.between(request.checkin(), request.checkout());
                if (nights <= 0) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Checkout must be after checkin");
                }

                BigDecimal roomTotal = room.getRoomType().getPrice().multiply(BigDecimal.valueOf(nights));

                // ✅ IMPORTANT: amount must not be null before first save
                booking.setAmount(roomTotal);

                // 6) Save booking first (get booking_id)
                Booking savedBooking = bookingRepository.save(booking);

                // 7) Add selected services (if any)
                BigDecimal servicesTotal = BigDecimal.ZERO;

                if (request.services() != null && !request.services().isEmpty()) {

                        for (BookingCreateRequest.ServiceItemRequest item : request.services()) {

                                HotelService hotelService = serviceRepository.findById(item.serviceId())
                                                .orElseThrow(() -> new ResponseStatusException(
                                                                HttpStatus.NOT_FOUND, "Service Not Found"));

                                int qty = (item.quantity() == null || item.quantity() < 1) ? 1 : item.quantity();

                                BigDecimal serviceTotal = hotelService.getPrice().multiply(BigDecimal.valueOf(qty));

                                BookingService bookingServiceEntity = new BookingService();
                                bookingServiceEntity.setBooking(savedBooking);
                                bookingServiceEntity.setService(hotelService);
                                bookingServiceEntity.setQuantity(qty);
                                bookingServiceEntity.setTotalAmount(serviceTotal);

                                serviceBookingRepository.save(bookingServiceEntity);

                                savedBooking.getBookingServices().add(bookingServiceEntity);

                                servicesTotal = servicesTotal.add(serviceTotal);
                        }
                }

                // 8) Final total = room + services
                savedBooking.setAmount(roomTotal.add(servicesTotal));

                Booking finalBooking = bookingRepository.save(savedBooking);

                // Notify via Telegram
                telegramNotifyService.sendNewBookingNotification(finalBooking);

                // 9) Return response
                return bookingMapper.toBookingResponse(finalBooking);
        }

        @Override
        public BookingResponse update(Integer id, BookingUpdateRequest request) {

                Booking booking = bookingRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Booking Not Found"));

                if (request.checkout().isBefore(request.checkin()) || request.checkout().isEqual(request.checkin())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "Check-out date must be after check-in date");
                }

                booking.setCheckin(request.checkin());
                booking.setCheckout(request.checkout());
                booking.setNumOfAdults(request.numOfAdults());
                booking.setNumOfChildren(request.numOfChildren());
                booking.setTotalNumOfGuest(request.numOfAdults() + request.numOfChildren());
                booking.setStatus(request.status());
                booking.setUpdatedAt(java.time.LocalDateTime.now());

                long nights = ChronoUnit.DAYS.between(request.checkin(), request.checkout());

                BigDecimal roomTotal = booking.getRoom().getRoomType().getPrice()
                                .multiply(BigDecimal.valueOf(nights));

                BigDecimal servicesTotal = booking.getBookingServices().stream()
                                .map(bs -> bs.getService().getPrice()
                                                .multiply(BigDecimal.valueOf(bs.getQuantity())))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                booking.setAmount(roomTotal.add(servicesTotal));

                Booking updatedBooking = bookingRepository.save(booking);

                return bookingMapper.toBookingResponse(updatedBooking);
        }

        @Override
        public BookingResponse findById(Integer id) {
                Booking booking = bookingRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Booking Not Found"));
                return bookingMapper.toBookingResponse(booking);
        }

        @Override
        public List<BookingResponse> findAll() {
                List<Booking> bookings = bookingRepository.findAll();
                return bookingMapper.toBookingResponseList(bookings);
        }

        @Override
        public void delete(Integer id) {
                Booking booking = bookingRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Booking Not Found"));
                bookingRepository.delete(booking);
        }

        @Override
        public List<BookingResponse> findBookingsByUserId(Integer userId) {
                // Validate user exists
                userRepository.findById(userId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "User Not Found"));

                // Get bookings
                List<Booking> bookings = bookingRepository.findAllByUserId(userId);

                // Return response list
                return bookingMapper.toBookingResponseList(bookings);
        }

    @Override
    public BookingResponse payBooking(Integer bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking Not Found"));

        if(!booking.getStatus().equalsIgnoreCase("pending")){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Booking already paid or invalid status");
        }

        // update booking status
        booking.setStatus("paid");

        // lock the room now
        Room room = booking.getRoom();
        room.setStatus("booked");
        roomRepository.save(room);

        Booking updatedBooking = bookingRepository.save(booking);

        return bookingMapper.toBookingResponse(updatedBooking);
    }
}
