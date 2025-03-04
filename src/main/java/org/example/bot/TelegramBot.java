package org.example.bot;

import org.example.ParseSite;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class TelegramBot extends TelegramLongPollingBot {
    final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    ParseSite parseSite = new ParseSite();
    private String bot_token;
    private String bot_name;

    public TelegramBot(){

    }

    public void loadConfig(){
        Properties properties = new Properties();
        try(FileInputStream fileInputStream = new FileInputStream("config.properties")){
            properties.load(fileInputStream);
            bot_token = properties.getProperty("bot_token");
            bot_name = properties.getProperty("bot_name");
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasCallbackQuery()){
            String data = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            try {
                sendResponse(update.getCallbackQuery().getMessage().getChatId(), data, messageId);
            }catch (IOException | TelegramApiException e){
                throw new RuntimeException(e);
            }
        }else if(update.getMessage().getText().equals("/start")){
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

    public void sendResponse(long chatId, String data, long messageID) throws IOException, TelegramApiException {
        EditMessageText message = new EditMessageText();
        message.setMessageId((int) messageID);
        switch (data){
            case "LessonButtonPressed":
                sendMessage(message, setLessonMenuButtons(), chatId, "Выберите дату");
                break;
            case "TodayLessonsButtonPressed":
                sendMessage(message, setLessonMenuButtons(), chatId, getLessons());
                break;
            case "TomorrowLessonsButtonPressed":
                sendMessage(message, setLessonMenuButtons(), chatId, getLessons(1));
                break;
            case "BackButtonPressed":
                sendMessage(message, setMainMenuButtons(), chatId, "Выберите функцию");
                break;
        }
    }
    //установка настроек сообщения
    private void sendMessage(EditMessageText message,InlineKeyboardMarkup keyboardMarkup, long chatId, String text) throws TelegramApiException {
        message.setReplyMarkup(keyboardMarkup);
        message.setChatId(chatId);
        message.setText(text);
        execute(message);
    }
    //получение расписания на завтра
    public String getLessons(int days) throws IOException {
        LocalDate localDate = LocalDate.now().plusDays(days);
        String day = localDate.format(dateTimeFormatter);
        return parseSite.getDay(day);
    }
    //получение расписания на сегодня
    public String getLessons() throws IOException {
        LocalDate localDate = LocalDate.now();
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
        return bot_name;
    }

    @Override
    public String getBotToken(){
        return bot_token;
    }
}
