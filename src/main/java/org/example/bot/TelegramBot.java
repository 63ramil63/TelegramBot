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
    final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
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
                message.setText("Выберите нужную для вас функцию");
                execute(message);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void sendResponse(Long chatId, String data) throws IOException, TelegramApiException {
        SendMessage message = new SendMessage();
        switch (data){
            case "LessonButtonPressed":
                message.setReplyMarkup(setLessonMenuButtons());
                message.setChatId(chatId);
                message.setText("Выберите дату");
                execute(message);
                break;
            case "TodayLessonsButtonPressed":
                message.setReplyMarkup(setLessonMenuButtons());
                message.setChatId(chatId);
                message.setText(getLessons(0));
                execute(message);
                break;
            case "TomorrowLessonsButtonPressed":
                message.setReplyMarkup(setLessonMenuButtons());
                message.setChatId(chatId);
                message.setText(getLessons(1));
                execute(message);
                break;
            case "BackButtonPressed":
                message.setReplyMarkup(setMainMenuButtons());
                message.setChatId(chatId);
                message.setText("Выберите функцию");
                execute(message);
                break;
        }
    }

    public String getLessons(int days) throws IOException {
        LocalDate localDate = LocalDate.now().plusDays(days);
        String day = localDate.format(dateTimeFormatter);
        return parseSite.getDay(day);
    }

    private InlineKeyboardMarkup setLessonMenuButtons(){
        InlineKeyboardButton today = setButton("На сегодня", "TodayLessonsButtonPressed");
        InlineKeyboardButton tomorrow = setButton("На завтра", "TomorrowLessonsButtonPressed");
        //создание кнопок и добавление к ним возвращщаемого значения при нажатии
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(today);
        row.add(tomorrow);
        //добавляем в ряд кнопки
        InlineKeyboardButton back = setButton("Назад", "BackButtonPressed");
        //кнопка для возвращения в глав меню
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(back);
        //добавление кнопки назад
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(row);
        keyboard.add(row1);
        //добавление рядов в клаву
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);
        //установка клавиатуры в markup
        return markup;
    }

    private InlineKeyboardButton setButton(String text, String callBackData){
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callBackData);
        return button;
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
