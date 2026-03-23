package com.Rothana.hotel_booking_system.features.telegram;

import com.Rothana.hotel_booking_system.entity.Booking;
import com.Rothana.hotel_booking_system.entity.TelegramLinkCode;
import com.Rothana.hotel_booking_system.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TelegramNotifyService {

    private final HotelBookingTelegramBot bot;
    private final TelegramBotProperties properties;
    private final TelegramLinkCodeRepository linkCodeRepository;

    public String generateLinkingUrl(User user) {
        String code = UUID.randomUUID().toString().substring(0, 8);
        TelegramLinkCode linkCode = new TelegramLinkCode();
        linkCode.setCode(code);
        linkCode.setUser(user);
        linkCode.setExpiresAt(java.time.LocalDateTime.now().plusHours(24));
        linkCodeRepository.save(linkCode);

        // e.g. https://t.me/BotName?start=CODE
        return String.format("https://t.me/%s?start=%s", properties.username(), code);
    }

    public void sendToChat(Long chatId, String text) {
        if (chatId == null)
            return;

        try {
            bot.execute(SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(text)
                    .parseMode("Markdown")
                    .build());
        } catch (TelegramApiException e) {
            System.err.println("Telegram notification failed for chat " + chatId + ": " + e.getMessage());
        }
    }

    public void sendRegistrationNotification(User user) {
        String text = String.format(
                "🆕 *New User Registered!*\n\n" +
                        "👤 *Name:* %s\n" +
                        "📧 *Email:* %s\n" +
                        "📞 *Phone:* %s\n" +
                        "📅 *Date:* %s",
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber() != null ? user.getPhoneNumber() : "N/A",
                user.getCreatedAt());

        // Notify Admin
        if (properties.adminChatId() != null) {
            sendToChat(properties.adminChatId(), text);
        }
    }

    public void sendNewBookingNotification(Booking booking) {
        String userEmail = booking.getUser() != null ? booking.getUser().getEmail() : "Unknown User";
        String userName = booking.getUser() != null ? booking.getUser().getName() : "Guest";

        String text = String.format(
                "📑 *New Booking Created!*\n\n" +
                        "👤 *Customer:* %s (%s)\n" +
                        "🆔 *Booking ID:* #%d\n" +
                        "🚪 *Room:* %s (Number: %d)\n" +
                        "💰 *Total Amount:* $%.2f\n" +
                        "📅 *Check-in:* %s\n" +
                        "📅 *Check-out:* %s\n\n" +
                        "Status: *Unpaid*",
                userName,
                userEmail,
                booking.getId(),
                booking.getRoom().getRoomType().getTypeName(),
                booking.getRoom().getId(),
                booking.getAmount(),
                booking.getCheckin(),
                booking.getCheckout());

        // Add services list if any
        if (booking.getBookingServices() != null && !booking.getBookingServices().isEmpty()) {
            StringBuilder servicesText = new StringBuilder("\n🛠 *Additional Services:*\n");
            for (var bs : booking.getBookingServices()) {
                servicesText.append(String.format("• %s (x%d) - $%.2f\n",
                        bs.getService().getServiceName(), bs.getQuantity(), bs.getTotalAmount()));
            }
            text += servicesText.toString();
        }

        // Notify Admin
        if (properties.adminChatId() != null) {
            sendToChat(properties.adminChatId(), text);
        }

        // Notify User if linked
        if (booking.getUser() != null && booking.getUser().getTelegramChatId() != null) {
            sendToChat(booking.getUser().getTelegramChatId(), text);
        }
    }

    public void sendPaymentNotification(Booking booking) {
        String userEmail = booking.getUser() != null ? booking.getUser().getEmail() : "Unknown User";
        String userName = booking.getUser() != null ? booking.getUser().getName() : "Guest";

        String text = String.format(
                "✅ *Payment Success!*\n\n" +
                        "👤 *Customer:* %s (%s)\n" +
                        "🆔 *Booking Number:* #%d\n" +
                        "🚪 *Room ID:* %d\n" +
                        "💰 *Room Price:* $%.2f / night\n" +
                        "💵 *Total Paid:* $%.2f\n" +
                        "📅 *Check-in:* %s\n" +
                        "📅 *Check-out:* %s\n",
                userName,
                userEmail,
                booking.getId(),
                booking.getRoom().getId(),
                booking.getRoom().getRoomType().getPrice(),
                booking.getAmount(),
                booking.getCheckin(),
                booking.getCheckout());

        // Add services list if any
        if (booking.getBookingServices() != null && !booking.getBookingServices().isEmpty()) {
            StringBuilder servicesText = new StringBuilder("\n🛠 *Additional Services:*\n");
            for (var bs : booking.getBookingServices()) {
                servicesText.append(String.format("• %s (x%d) - $%.2f\n",
                        bs.getService().getServiceName(), bs.getQuantity(), bs.getTotalAmount()));
            }
            text += servicesText.toString();
        }

        text += "\nThank you for booking with us! 😊";

        String imageUrl = booking.getRoom().getImage();

        // 1. Send to Admin if configured
        if (properties.adminChatId() != null) {
            sendInternal(properties.adminChatId(), text, imageUrl);
        }

        // 2. Send to the specific User if they have linked Telegram
        if (booking.getUser() != null && booking.getUser().getTelegramChatId() != null) {
            sendInternal(booking.getUser().getTelegramChatId(), text, imageUrl);
        }
    }

    private void sendInternal(Long chatId, String text, String imageUrl) {
        if (imageUrl != null && !imageUrl.isBlank()) {
            try {
                bot.execute(SendPhoto.builder()
                        .chatId(chatId.toString())
                        .photo(new InputFile(imageUrl))
                        .caption(text)
                        .parseMode("Markdown")
                        .build());
            } catch (TelegramApiException e) {
                // Fallback to text
                sendToChat(chatId, text);
            }
        } else {
            sendToChat(chatId, text);
        }
    }
}
