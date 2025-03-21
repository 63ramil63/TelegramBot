package org.example.messages;

import org.example.ParseSite;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Messages {
    public static void sendMessage(SendMessage message, long chatId, String text) throws TelegramApiException {
        //для отправки сообщения без кнопки
        message.setChatId((Long) chatId);
        message.setText(text);
    }

    //установка настроек сообщения
    public static void sendMessage(SendMessage message, InlineKeyboardMarkup keyboardMarkup, long chatId, String text) throws TelegramApiException {
        //для ОТПРАВКИ нового сообщения
        message.setReplyMarkup(keyboardMarkup);
        message.setChatId((Long) chatId);
        message.setText(text);
    }

    //установка настроек сообщения
    public static void editMessage(EditMessageText message, InlineKeyboardMarkup keyboardMarkup, long chatId, String text) throws TelegramApiException {
        //для ИЗМЕНЕНИЯ существующего сообщения
        message.setReplyMarkup(keyboardMarkup);
        message.setChatId((Long) chatId);
        message.setText(text);
    }

    public static DeleteMessage deleteMessage(long chatId, int messageId) throws TelegramApiException {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId((Long) chatId);
        deleteMessage.setMessageId(Integer.valueOf(messageId));
        return deleteMessage;
    }

    public static InlineKeyboardMarkup setLessonMenuButtons(){
        InlineKeyboardButton today = setButton("На сегодня", "TodayLessonsButtonPressed");
        InlineKeyboardButton tomorrow = setButton("На завтра", "TomorrowLessonsButtonPressed");
        //создание кнопок и добавление к ним возвращаемого значения при нажатии
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

    public static InlineKeyboardButton setButton(String text, String callBackData){
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callBackData);
        return button;
    }

    public static InlineKeyboardMarkup setMainMenuButtons(){
        List<InlineKeyboardButton> row = new ArrayList<>();
        //создание массива кнопок
        InlineKeyboardButton lessonButton = setButton("Расписание", "LessonButtonPressed");
        InlineKeyboardButton fileButton = setButton("Файлы", "FileButtonPressed");
        //создание кнопки и установка текста и возвращаемого значения при нажатии
        row.add(fileButton);
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

    public static InlineKeyboardMarkup setSelectYearButtons() throws IOException {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        for(String year: ParseSite.getYear()){
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = setButton(year, year + "year");
            row.add(button);
            keyboard.add(row);
        }
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton back = setButton("Назад", "BackButtonPressed");
        row.add(back);
        keyboard.add(row);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);
        return markup;
    }
}
