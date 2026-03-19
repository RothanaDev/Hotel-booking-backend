package com.Rothana.hotel_booking_system.features.Payments;

import com.Rothana.hotel_booking_system.entity.Payment;
import com.Rothana.hotel_booking_system.features.Payments.dto.PaymentResponse;
import com.Rothana.hotel_booking_system.features.Payments.dto.PaypalCaptureRequest;
import com.Rothana.hotel_booking_system.features.Payments.dto.PaypalCaptureResponse;
import com.Rothana.hotel_booking_system.features.Payments.dto.PaypalCreateOrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/payments/paypal")
@RequiredArgsConstructor
public class PaypalPaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create/{bookingId}")
    public PaypalCreateOrderResponse create(@PathVariable Integer bookingId) {
        return paymentService.createPaypalOrder(bookingId);
    }

    @PostMapping("/capture")
    public PaypalCaptureResponse capture(@RequestBody PaypalCaptureRequest request) {
        return paymentService.capturePaypalOrder(request);
    }

    @GetMapping("/success")
    public ResponseEntity<?> success(@RequestParam("token") String orderId) {
        // auto-capture when PayPal returns success
        PaypalCaptureResponse captured = paymentService.capturePaypalOrder(new PaypalCaptureRequest(orderId));
        return ResponseEntity.ok(captured);
    }

    @GetMapping("/cancel")
    public ResponseEntity<?> cancel(@RequestParam("token") String orderId) {
        return ResponseEntity.ok("Payment cancelled. orderId=" + orderId);
    }

    @GetMapping("/success-redirect")
    public ResponseEntity<Void> successRedirect(@RequestParam("token") String orderId) {
        return ResponseEntity.status(302)
                .location(URI.create("http://localhost:3000/paypal/success?token=" + orderId))
                .build();
    }

    @GetMapping("/cancel-redirect")
    public ResponseEntity<Void> cancelRedirect(@RequestParam("token") String orderId) {
        return ResponseEntity.status(302)
                .location(URI.create("http://localhost:3000/paypal/cancel?token=" + orderId))
                .build();
    }
    @PostMapping("/cash/{bookingId}")
    public ResponseEntity<String> payCash(@PathVariable Integer bookingId) {

        paymentService.payCash(bookingId);

        return ResponseEntity.ok("Cash payment completed");
    }
    @GetMapping("/all")
    public ResponseEntity<List<PaymentResponse>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }
}
