package org.example.messages;

import org.example.ParseSite;
import org.example.bot.TelegramBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Messages {
    /**
     * setting new message
     * @param message SendMessage
     * @param chatId chatId
     * @param text message text
     */
    public static void sendMessage(SendMessage message, long chatId, String text) {
        //для отправки сообщения без кнопки
        message.setChatId(chatId);
        message.setText(text);
    }

    /**
     * setting new message
     * @param message SendMessage
     * @param keyboardMarkup InlineKeyboardMarkup
     * @param chatId chatId
     * @param text message text
     */
    public static void sendMessage(SendMessage message, InlineKeyboardMarkup keyboardMarkup, long chatId, String text) throws TelegramApiException {
        //для ОТПРАВКИ нового сообщения
        message.setReplyMarkup(keyboardMarkup);
        message.setChatId(chatId);
        message.setText(text);
    }

    /**
     * setting edit message
     * @param message EditMessage
     * @param keyboardMarkup InlineKeyboardMarkup
     * @param chatId chatId
     * @param text message text
     */
    public static void editMessage(EditMessageText message, InlineKeyboardMarkup keyboardMarkup, long chatId, String text) {
        //для ИЗМЕНЕНИЯ существующего сообщения
        message.setReplyMarkup(keyboardMarkup);
        message.setChatId(chatId);
        message.setText(text);
    }

    /**
     * setting message that will be deleted
     * @param deleteMessage DeleteMessage
     * @param chatId chatId
     * @param messageId messageId
     */
    public static void deleteMessage(DeleteMessage deleteMessage, long chatId, int messageId) throws TelegramApiException {
        deleteMessage.setChatId(chatId);
        deleteMessage.setMessageId(messageId);
    }

    /**
     * @param buttons buttons that will be added to the row
     * @return row with buttons that you added to the method
     */
    public static List<InlineKeyboardButton> setRow(InlineKeyboardButton ... buttons) {
        List<InlineKeyboardButton> row = new ArrayList<>(Arrays.asList(buttons));
        return row;
    }

    /**
     * @param text button text
     * @param callBackData value that returns when button pressed
     * @return customized button
     */
    public static InlineKeyboardButton setButton(String text, String callBackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callBackData);
        return button;
    }

    public static InlineKeyboardMarkup setLessonMenuButtons() {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        //создание кнопок и добавление к ним возвращаемого значения при нажатии
        InlineKeyboardButton today = setButton("На сегодня", "TodayLessonsButtonPressed");
        InlineKeyboardButton tomorrow = setButton("На завтра", "TomorrowLessonsButtonPressed");
        keyboard.add(setRow(today, tomorrow));

        InlineKeyboardButton selectYear = setButton("Выбрать курс", "SelectYearButtonPressed");
        //кнопка для выбора курса
        keyboard.add(setRow(selectYear));

        //кнопка для возвращения в глав меню
        InlineKeyboardButton back = setButton("Назад", "BackButtonPressed");
        keyboard.add(setRow(back));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);
        return markup;
    }

    public static InlineKeyboardMarkup setMainMenuButtons() {
        //создание кнопки и установка текста и возвращаемого значения при нажатии
        InlineKeyboardButton lessonButton = setButton("Расписание", "LessonButtonPressed");
        InlineKeyboardButton fileButton = setButton("Файлы", "FileButtonPressed");

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        //добавляем ряд кнопок в клавиатуру
        keyboard.add(setRow(fileButton, lessonButton));

        InlineKeyboardButton helpButton = setButton("Помощь", "/help");
        keyboard.add(setRow(helpButton));

        //создание самого объекта клавиатуры, к которому все добавляем
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);
        return markup;
    }

    /**
     * @param text text like "Четвертый курс:Year8" or "ИКС-4Group=124"
     * @param indexValue - substring that will be used to set correct value to button like 'Group' or 'Year'
     * @return row with button
     */
    private static List<InlineKeyboardButton> setRow(String text, String indexValue) {
        int index = text.indexOf(indexValue);
        //индекс, который указывает на element
        String num = text.substring(index);
        text = text.replace(num, "");
        //удаляем из строки element(что то там)
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton button = setButton(text, text + num);
        row.add(button);
        return row;
    }

    /**
     * Get keyboard to select year of study
     */
    public static void setSelectYearButtons() throws IOException {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<String> years = ParseSite.getYear();
        for (String year : years) {
            keyboard.add(setRow(year, "Year"));
        }
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton back = setButton("Назад", "BackButtonPressed");
        row.add(back);
        keyboard.add(row);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);
        //сохраняем значение в HashMap
        TelegramBot.yearsAndGroupsCache.put("Year", markup);
    }

    /**
     * Get keyboard to select group
     */
    public static void setGroupSelectButtons(int i) throws IOException {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<String> groups = ParseSite.getGroups(i);
        for (String group : groups) {
            keyboard.add(setRow(group, "Group"));
        }
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton back = setButton("Назад", "BackButtonPressed");
        row.add(back);
        keyboard.add(row);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);
        TelegramBot.yearsAndGroupsCache.put("Groups" + i, markup);
    }
}