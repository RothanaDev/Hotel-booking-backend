
package com.Rothana.hotel_booking_system.features.Payments;

import com.Rothana.hotel_booking_system.entity.Payment;
import com.Rothana.hotel_booking_system.features.Payments.dto.PaymentResponse;
import com.Rothana.hotel_booking_system.features.Payments.dto.PaypalCaptureRequest;
import com.Rothana.hotel_booking_system.features.Payments.dto.PaypalCaptureResponse;
import com.Rothana.hotel_booking_system.features.Payments.dto.PaypalCreateOrderResponse;

import java.util.List;

public interface PaymentService {
    PaypalCreateOrderResponse createPaypalOrder(Integer bookingId);
    PaypalCaptureResponse capturePaypalOrder(PaypalCaptureRequest request);
    void payCash(Integer bookingId);
    List<PaymentResponse> getAllPayments();

}
