package org.example.bot;

import org.example.Main;
import org.example.config.Config;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class TelegramBot extends TelegramLongPollingBot {

    @Override
    public void onUpdateReceived(Update update) {
        SendMessage message = new SendMessage();
        message.setChatId(update.getMessage().getChatId());
        try {
            message.setText(getLessons());
            execute(message);
        } catch (TelegramApiException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getLessons() throws IOException {
        LocalDate localDate = LocalDate.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String day = localDate.format(dateTimeFormatter);
        return Main.getDay(day);
    }

    @Override
    public String getBotUsername() {
        return Config.getBotName();
    }

    @Override
    public String getBotToken(){
        return Config.getToken();
    }
}
