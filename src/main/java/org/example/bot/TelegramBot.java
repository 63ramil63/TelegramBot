package org.example.bot;

import org.example.Files.FilesAndFolders;
import org.example.ParseSite;
import org.example.messages.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

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
    public static HashMap<Long, String> selectedPath = new HashMap<>();
    public static HashMap<Long, Boolean> canAddFolder = new HashMap<>();
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

            //удаление статуса создания папки
            canAddFolder.remove(chatId);

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
                    DeleteMessage deleteMessage = Messages.deleteMessage(chatId, messageId);
                    execute(deleteMessage);
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
            System.out.println("hasDoc");
            //удаление статуса создания папки
            canAddFolder.remove(chatId);

            if(selectedPath.containsKey((Long) chatId) && !selectedPath.get((Long) chatId).isEmpty()) {
                Document document = update.getMessage().getDocument();
                System.out.println("doc");
                saveFile(document, chatId, update.getMessage().getCaption());
            }else{
                System.out.println("sendMessage");
                SendMessage message = new SendMessage();
                try {
                    Messages.sendMessage(message, FilesAndFolders.getFilesFromFolder(path), chatId, "Сначала выберите папку куда будете сохранять");
                    execute(message);
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
            System.out.println(fileName + "filename");
            String extension = fileName.substring(fileName.lastIndexOf("."));
            //получаем расширение файла

            if(text != null) {
                System.out.println(1);
                Files.copy(is, Paths.get(selectedPath.get((Long) chatId) + "\\" + text +  extension));
            }else{
                System.out.println(2);
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
                Messages.sendMessage(message, chatid, "Файл сохранён");
                execute(message);
                //отправка сообщения без кнопки
                sendNewMessageResponse(chatid, "/start");
                //отправка сообщения с кнопками
                break;
            case "/start":
                Messages.sendMessage(message, Messages.setMainMenuButtons(), chatid, "Выберите функцию");
                execute(message);
                break;
            case "/error":
                Messages.sendMessage(message, Messages.setMainMenuButtons(), chatid, "Произошла ошибка(");
                execute(message);
                break;
            default:
                try {
                    FilesAndFolders.addFolder(data);
                    Messages.sendMessage(message, Messages.setMainMenuButtons(), chatid, "Директория создана");
                    execute(message);
                    canAddFolder.remove(chatid);
                }catch (IOException e){
                    sendNewMessageResponse(chatid, "/error");
                    System.out.println(e);
                }
                break;
        }
    }


    public void sendEditMessageResponse(long chatId, String data, long messageID) throws IOException, TelegramApiException {
        //метод для ИЗМЕНЕНИЯ существующего сообщения
        EditMessageText message = new EditMessageText();
        message.setMessageId(Integer.valueOf((int) messageID));
        switch (data) {
            case "LessonButtonPressed":
                System.out.println("LessonPressed");
                Messages.editMessage(message, Messages.setLessonMenuButtons(), chatId, "Выберите дату");
                execute(message);
                break;
            case "TodayLessonsButtonPressed":
                Messages.editMessage(message, Messages.setLessonMenuButtons(), chatId, getLessons());
                execute(message);
                break;
            case "TomorrowLessonsButtonPressed":
                Messages.editMessage(message, Messages.setLessonMenuButtons(), chatId, getLessons(1));
                execute(message);
                break;
            case "BackButtonPressed":
                Messages.editMessage(message, Messages.setMainMenuButtons(), chatId, "Выберите функцию");
                execute(message);
                break;
            case "FileButtonPressed":
                Messages.editMessage(message, FilesAndFolders.getFilesFromFolder(path), chatId, "Выберите вашу папку вашей группы");
                execute(message);
                break;
            case "AddFolderButtonPressed":
                Messages.editMessage(message, Messages.setMainMenuButtons(), chatId, "Напишите название файла");
                execute(message);
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
            Messages.editMessage(message, FilesAndFolders.getFilesFromFolder(path1), chatId, "Выберите файл или загрузите его");
            execute(message);
            if(selectedPath.containsKey((Long) chatId)){
                selectedPath.replace(chatId, path1);
            }else{
                selectedPath.put((Long) chatId, path1);
            }
        }
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