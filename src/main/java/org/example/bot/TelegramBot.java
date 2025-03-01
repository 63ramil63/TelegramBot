package org.example.bot;

import org.example.ParseSite;
import org.example.config.Config;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TelegramBot extends TelegramLongPollingBot {

    ParseSite parseSite = new ParseSite();

    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasCallbackQuery()){
            String data = update.getCallbackQuery().getData();
            try {
                sendResponse(update.getCallbackQuery().getMessage().getChatId(), data);
            }catch (IOException | TelegramApiException e){
                throw new RuntimeException(e);
            }
        }else{
            SendMessage message = new SendMessage();
            message.setReplyMarkup(setMainMenuButtons());
            message.setChatId(update.getMessage().getChatId());
            try {
                message.setText(getLessons());
                execute(message);
            } catch (TelegramApiException | IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void sendResponse(Long chatId, String data) throws IOException, TelegramApiException {
        if(data.equals("LessonButtonPressed")){
            SendMessage message = new SendMessage();
            message.setReplyMarkup(setMainMenuButtons());
            message.setChatId(chatId);
            message.setText(getLessons());
            execute(message);
        }
    }

    public String getLessons() throws IOException {
        LocalDate localDate = LocalDate.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String day = localDate.format(dateTimeFormatter);
        return parseSite.getDay(day);
    }

    private InlineKeyboardMarkup setMainMenuButtons(){
        List<InlineKeyboardButton> row = new ArrayList<>();
        //создание массива кнопок
        InlineKeyboardButton lessonButton = new InlineKeyboardButton();
        lessonButton.setText("Расписание");
        lessonButton.setCallbackData("LessonButtonPressed");
        //создание кнопки и установка текста и возвращаемого значения при нажатии
        row.add(lessonButton);
        //добавляем кнопку в массив кнопок
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(row);
        //клавиатура является массивом в массиве кнопок
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);
        //создание самого объекта клавиатуры, к которому все добавляем
        return markup;
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
