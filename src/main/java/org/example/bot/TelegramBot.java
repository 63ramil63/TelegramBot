package org.example.bot;

import org.example.Files.FilesAndFolders;
import org.example.ParseSite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;



public class TelegramBot extends TelegramLongPollingBot {
    private static final Logger log = LoggerFactory.getLogger(TelegramBot.class);
    final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    ParseSite parseSite = new ParseSite();
    private final HashMap<String, CachedLessons> cache = new HashMap<>();
    public HashMap<Long, String> selectedPath = new HashMap<>();
    public HashMap<Long, Boolean> canAddFolder = new HashMap<>();
    private String bot_token;
    private String bot_name;
    private String duration;
    public static String path;

    public TelegramBot(){
        loadConfig();
    }

    public void loadConfig(){
        Properties properties = new Properties();
        try(FileInputStream fileInputStream = new FileInputStream("src/main/resources/config.properties")){
            properties.load(fileInputStream);
            bot_token = properties.getProperty("bot_token");
            bot_name = properties.getProperty("bot_name");
            duration = properties.getProperty("duration");
            path = properties.getProperty("path");
        }catch (IOException e){
            log.error("e: ", e);
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasCallbackQuery()){
            //getCallBackQuery дает те же возможности что и message, но получить message можно только из CallBackQuery.getMessage
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            String data = update.getCallbackQuery().getData();
            System.out.println(data);
            if(data.contains("Folder")){
                try {
                    long messageId = update.getCallbackQuery().getMessage().getMessageId();
                    sendEditMessageResponse(chatId, data, messageId);
                }catch (IOException | TelegramApiException e){
                    throw new RuntimeException(e);
                }
            }else if(data.equals("FileButtonPressed")){
                try {
                    long messageId = update.getCallbackQuery().getMessage().getMessageId();
                    sendEditMessageResponse(chatId, data, messageId);
                }catch (IOException | TelegramApiException e){
                    throw new RuntimeException(e);
                }
            }else if(data.contains("File")){
                try {
                    int messageId = update.getCallbackQuery().getMessage().getMessageId();
                    deleteMessage(chatId, messageId);
                    SendDocument sendDocument = FilesAndFolders.sendMessageWithDoc(data, chatId);
                    execute(sendDocument);
                    sendNewMessageResponse(chatId, "/start");
                } catch (TelegramApiException e) {
                    try {
                        sendNewMessageResponse(chatId, "/error");
                    } catch (TelegramApiException ex) {
                        throw new RuntimeException(ex);
                    }
                    System.out.println(e);
                    throw new RuntimeException(e);
                }
            }else{
                try {
                    long messageId = update.getCallbackQuery().getMessage().getMessageId();
                    sendEditMessageResponse(chatId, data, messageId);
                } catch (IOException | TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
        }else if(update.getMessage().hasDocument()){
            long chatId = update.getMessage().getChatId();
            if(selectedPath.containsKey((Long) chatId) && !selectedPath.get((Long) chatId).isEmpty()) {
                Document document = update.getMessage().getDocument();
                saveFile(document, chatId, update.getMessage().getCaption());
            }else{
                SendMessage message = new SendMessage();
                try {
                    sendMessage(message, FilesAndFolders.getFilesFromFolder(path), chatId, "Сначала выберите папку куда будете сохранять");
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
        }else if(update.getMessage().getText().equals("/start")){
            long chatId = update.getMessage().getChatId();
            try {
                sendNewMessageResponse(chatId, "/start");
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }else if(update.getMessage().hasText()){
            long chatId = update.getMessage().getChatId();
            String text = update.getMessage().getText();
            if(canAddFolder.get(chatId)){
                try {
                    sendNewMessageResponse(chatId, text);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void saveFile(Document document, long chatId, String text){
        try {
            String fileId = document.getFileId();
            String filePath = execute(new GetFile(fileId)).getFilePath();
            //обращение к TelegramAPI для получения инфы о файле

            String fullFilePath = "https://api.telegram.org/file/bot" + bot_token + "/" + filePath;
            InputStream is = new URL(fullFilePath).openStream();
            //открываем поток для чтения

            String fileName = document.getFileName();
            String extension = fileName.substring(fileName.lastIndexOf("."));
            //получаем расширение файла

            if(!text.isEmpty()) {
                Files.copy(is, Paths.get(selectedPath.get((Long) chatId) + "\\" + text +  extension));
            }else{
                Files.copy(is, Paths.get(selectedPath.get((Long) chatId) + "\\" + fileName));
            }
            //копируем файл из потока в путь
            is.close();
            sendNewMessageResponse(chatId, "FileSaved");

        } catch (TelegramApiException | IOException e) {
            throw new RuntimeException(e);
        }
    }



    private void sendNewMessageResponse(long chatid, String data) throws TelegramApiException{
        //метод для ОТПРАВКИ сообщения
        SendMessage message = new SendMessage();
        message.setChatId((Long) chatid);
        switch (data){
            case "FileSaved":
                sendMessage(message, chatid, "Файл сохранён");
                //отправка сообщения без кнопки
                sendNewMessageResponse(chatid, "/start");
                //отправка сообщения с кнопками
                break;
            case "/start":
                sendMessage(message, setMainMenuButtons(), chatid, "Выберите функцию");
                break;
            case "/error":
                sendMessage(message, setMainMenuButtons(), chatid, "Произошла ошибка(");
                break;
            default:
                try {
                    FilesAndFolders.addFolder(data);
                }catch (IOException e){
                    System.out.println(e);
                }
                break;
        }
    }




    private void sendMessage(SendMessage message, long chatId, String text) throws TelegramApiException{
        //для отправки сообщения без кнопки
        message.setChatId((Long) chatId);
        message.setText(text);
        execute(message);
    }

    //установка настроек сообщения
    private void sendMessage(SendMessage message, InlineKeyboardMarkup keyboardMarkup, long chatId, String text) throws TelegramApiException {
        //для ОТПРАВКИ нового сообщения
        message.setReplyMarkup(keyboardMarkup);
        message.setChatId((Long) chatId);
        message.setText(text);
        execute(message);
    }





    public void sendEditMessageResponse(long chatId, String data, long messageID) throws IOException, TelegramApiException {
        //метод для ИЗМЕНЕНИЯ существующего сообщения
        EditMessageText message = new EditMessageText();
        message.setMessageId(Integer.valueOf((int) messageID));
        switch (data) {
            case "LessonButtonPressed":
                System.out.println("LessonPressed");
                editMessage(message, setLessonMenuButtons(), chatId, "Выберите дату");
                break;
            case "TodayLessonsButtonPressed":
                editMessage(message, setLessonMenuButtons(), chatId, getLessons());
                break;
            case "TomorrowLessonsButtonPressed":
                editMessage(message, setLessonMenuButtons(), chatId, getLessons(1));
                break;
            case "BackButtonPressed":
                editMessage(message, setMainMenuButtons(), chatId, "Выберите функцию");
                break;
            case "FileButtonPressed":
                editMessage(message, FilesAndFolders.getFilesFromFolder(path), chatId, "Выберите вашу папку вашей группы");
                break;
            case "AddFolderButtonPressed":
                editMessage(message, setMainMenuButtons(), chatId, "Напишите название файла");
                if(canAddFolder.containsKey(chatId)){
                    canAddFolder.remove(chatId);
                    canAddFolder.put(chatId, true);
                }else{
                    canAddFolder.put(chatId, true);
                }
                break;
        }

        //если нажали на кнопку директории
        if(data.contains("Folder")){
            String path1 = data.replace("Folder", "");
            System.out.println(path1);
            editMessage(message, FilesAndFolders.getFilesFromFolder(path1), chatId, "Выберите файл или загрузите его");
            if(selectedPath.containsKey((Long) chatId)){
                selectedPath.remove((Long) chatId);
            }else{
                selectedPath.put((Long) chatId, path1);
            }
        }
    }

    //установка настроек сообщения
    private void editMessage(EditMessageText message,InlineKeyboardMarkup keyboardMarkup, long chatId, String text) throws TelegramApiException {
        //для ИЗМЕНЕНИЯ существующего сообщения
        message.setReplyMarkup(keyboardMarkup);
        message.setChatId((Long) chatId);
        message.setText(text);
        execute(message);
    }


    //получение расписания на завтра
    public String getLessons(int days) throws IOException {
        System.out.println("getLessons");
        if(cache.containsKey("Tomorrow") && !cache.get("Tomorrow").isExpired()){
            System.out.println("Возвращение из кэша Tomorrow");
            return cache.get("Tomorrow").lessons;
        }else if(cache.containsKey("Tomorrow") && cache.get("Tomorrow").isExpired()){
            cache.remove("Tomorrow");
        }
        LocalDate localDate = LocalDate.now().plusDays(days);
        String day = localDate.format(dateTimeFormatter);
        String lesson = parseSite.getDay(day);
        cache.put("Tomorrow", new CachedLessons(lesson, Long.parseLong(duration)));
        System.out.println("Сохранение в кэш: " + cache.get("Tomorrow").toString());
        return lesson;
    }

    //получение расписания на сегодня
    public String getLessons() throws IOException {
        if(cache.containsKey("Today") && !cache.get("Today").isExpired()){
            System.out.println("Возвращение из кэша Today");
            return cache.get("Today").lessons;
        }else if(cache.containsKey("Today") && cache.get("Today").isExpired()){
            cache.remove("Today");
        }
        LocalDate localDate = LocalDate.now();
        String day = localDate.format(dateTimeFormatter);
        String lesson = parseSite.getDay(day);
        cache.put("Today", new CachedLessons(lesson, Long.parseLong(duration)));
        System.out.println("Сохранение в кэш: " + cache.get("Today").toString());
        return lesson;
    }

    private static InlineKeyboardMarkup setLessonMenuButtons(){
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

    private static InlineKeyboardMarkup setMainMenuButtons(){
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

    public void deleteMessage(long chatId, int messageId) throws TelegramApiException {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId((Long) chatId);
        deleteMessage.setMessageId(Integer.valueOf(messageId));
        System.out.println(messageId);
        execute(deleteMessage);
    }


    @Override
    public String getBotUsername() {
        return bot_name;
    }

    @Override
    public String getBotToken(){
        return bot_token;
    }

    private static class CachedLessons{
        String lessons;
        long timestamp;
        long duration;

        public CachedLessons(String lessons, long duration){
            this.timestamp = System.currentTimeMillis();
            this.lessons = lessons;
            this.duration = duration;
        }

        public boolean isExpired(){
            return System.currentTimeMillis() - timestamp > TimeUnit.MINUTES.toMillis(duration);
            //текущее время - время создания > времени существования
        }
    }
}