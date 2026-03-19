package com.Rothana.hotel_booking_system.features.Payments;

import com.Rothana.hotel_booking_system.entity.*;
import com.Rothana.hotel_booking_system.features.Payments.dto.*;
import com.Rothana.hotel_booking_system.features.booking.BookingRepository;
import com.Rothana.hotel_booking_system.features.telegram.TelegramNotifyService;
import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.http.exceptions.HttpException;
import com.paypal.orders.*;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PayPalHttpClient payPalHttpClient;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final TelegramNotifyService telegramNotifyService;

    @Value("${app.currency:USD}")
    private String currency;

    @Value("${app.paypal.return-url:http://localhost:3000/paypal/success}")
    private String returnUrl;

    @Value("${app.paypal.cancel-url:http://localhost:3000/paypal/cancel}")
    private String cancelUrl;



    @Transactional
    @Override
    public PaypalCreateOrderResponse createPaypalOrder(Integer bookingId) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking Not Found"));

        BigDecimal amount = booking.getAmount();

        String cur = currency == null ? "USD" : currency.trim().toUpperCase();
        String amountString = amount.setScale(2, RoundingMode.HALF_UP).toPlainString();

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.checkoutPaymentIntent("CAPTURE");

        Money money = new Money()
                .currencyCode(cur)
                .value(amountString);

        Item item = new Item()
                .name("Hotel booking #" + booking.getId())
                .quantity("1")
                .unitAmount(money);

        AmountBreakdown breakdown = new AmountBreakdown().itemTotal(money);

        AmountWithBreakdown amountWithBreakdown = new AmountWithBreakdown()
                .currencyCode(cur)
                .value(amountString)
                .amountBreakdown(breakdown);

        PurchaseUnitRequest purchaseUnitRequest = new PurchaseUnitRequest()
                .referenceId("BOOKING_" + booking.getId())
                .description("Hotel booking payment")
                .amountWithBreakdown(amountWithBreakdown)
                .items(List.of(item));

        ApplicationContext applicationContext = new ApplicationContext()
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .brandName("Hotel Booking System")
                .userAction("PAY_NOW")
                .shippingPreference("NO_SHIPPING");

        orderRequest.purchaseUnits(List.of(purchaseUnitRequest));
        orderRequest.applicationContext(applicationContext);

        OrdersCreateRequest request = new OrdersCreateRequest();
        request.requestBody(orderRequest);

        try {

            HttpResponse<Order> response = payPalHttpClient.execute(request);
            Order order = response.result();

            String approvalUrl = order.links().stream()
                    .filter(l -> "approve".equalsIgnoreCase(l.rel()))
                    .findFirst()
                    .map(LinkDescription::href)
                    .orElse(null);

            if (approvalUrl == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "PayPal approval url not found");
            }

            Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);

            if (payment == null) {

                payment = new Payment();
                payment.setBooking(booking);
                payment.setAmount(amount);
                payment.setCurrency(cur);
                payment.setProvider(PaymentProvider.PAYPAL); // IMPORTANT

            }

            payment.setStatus(PaymentStatus.CREATED);
            payment.setPaypalOrderId(order.id());

            paymentRepository.save(payment);

            return new PaypalCreateOrderResponse(order.id(), approvalUrl);

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to create PayPal order: " + e.getMessage());
        }
    }

@Transactional
@Override
public PaypalCaptureResponse capturePaypalOrder(PaypalCaptureRequest captureRequest) {

    String orderId = captureRequest.orderId();

    Payment payment = paymentRepository.findByPaypalOrderId(orderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

    if (payment.getStatus() == PaymentStatus.COMPLETED) {
        return new PaypalCaptureResponse(orderId, payment.getPaypalCaptureId(), "COMPLETED");
    }

    OrdersCaptureRequest request = new OrdersCaptureRequest(orderId);
    request.requestBody(new OrderRequest());

    try {

        HttpResponse<Order> response = payPalHttpClient.execute(request);
        Order order = response.result();

        String paypalStatus = order.status();

        String captureId = null;

        if (order.purchaseUnits() != null && !order.purchaseUnits().isEmpty()) {
            PurchaseUnit pu = order.purchaseUnits().get(0);

            if (pu.payments() != null && pu.payments().captures() != null
                    && !pu.payments().captures().isEmpty()) {

                captureId = pu.payments().captures().get(0).id();
            }
        }

        if ("COMPLETED".equalsIgnoreCase(paypalStatus)) {

            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setPaypalCaptureId(captureId);

            Booking booking = payment.getBooking();
            booking.setStatus("paid");

            Room room = booking.getRoom();
            room.setStatus("booked");

            bookingRepository.save(booking);

            telegramNotifyService.sendPaymentNotification(booking);
        }

        paymentRepository.save(payment);

        return new PaypalCaptureResponse(orderId, captureId, paypalStatus);

    } catch (Exception e) {

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);

        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to capture PayPal order: " + e.getMessage());
    }
}

    @Override
    public void payCash(Integer bookingId) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if ("paid".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException("Booking already paid");
        }

        // create payment record
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setProvider(PaymentProvider.CASH);
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setAmount(booking.getAmount());
        payment.setCurrency("USD");

        paymentRepository.save(payment);

        // update booking
        booking.setStatus("paid");

        // update room
        Room room = booking.getRoom();
        room.setStatus("booked");

        bookingRepository.save(booking);

        // send telegram notification
        telegramNotifyService.sendPaymentNotification(booking);
    }

    @Override
    public List<PaymentResponse> getAllPayments() {

        return paymentRepository.findAll()
                .stream()
                .map(payment -> new PaymentResponse(

                        payment.getId(),
                        payment.getBooking().getId(),

                        new PaymentUserDTO(
                                payment.getBooking().getUser().getId(),
                                payment.getBooking().getUser().getName(),
                                payment.getBooking().getUser().getEmail()
                        ),

                        payment.getProvider().name(),
                        payment.getStatus().name(),
                        payment.getAmount(),
                        payment.getCurrency(),
                        payment.getCreatedAt()

                ))
                .toList();
    }
}
