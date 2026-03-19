package com.Rothana.hotel_booking_system.features.telegram;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

// @Component // REMOVED: Using telegrambots-spring-boot-starter registers the bot automatically!
@RequiredArgsConstructor
public class TelegramBotInitializer {

    private final HotelBookingTelegramBot bot;

    // @PostConstruct
    public void init() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(bot);
            System.out.println("✅ Telegram bot registered!");
        } catch (TelegramApiException e) {
            System.err.println("❌ Telegram bot failed to register: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
