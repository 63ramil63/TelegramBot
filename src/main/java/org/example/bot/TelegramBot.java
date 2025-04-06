package org.example.bot;

import com.fasterxml.jackson.databind.jsontype.impl.AsDeductionTypeDeserializer;
import org.example.Files.FilesAndFolders;
import org.example.Main;
import org.example.ParseSite;
import org.example.database.UserRepository;
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
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class TelegramBot extends TelegramLongPollingBot {
    private static final Logger log = LoggerFactory.getLogger(TelegramBot.class);
    final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private final HashMap<String, CachedLessons> lessonsCache = new HashMap<>();
    public static HashMap<String, InlineKeyboardMarkup> yearsAndGroupsCache = new HashMap<>();
    private String bot_token;
    private String bot_name;
    private String duration;
    public static String path;
    //newCachedThreadPool - создает пул потоков, который может создавать новые потоки по мере необходимости, но при этом повторно использовать ранее созданные потоки, если они свободны
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public TelegramBot(){
        loadConfig();
    }

    //загрузка данных из файла конфига
    public void loadConfig(){
        Properties properties = new Properties();
        try(FileInputStream fileInputStream = new FileInputStream(Main.propertyPath)){
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
    public void onUpdateReceived(Update update){
        //создаем/используем поток при получении сообщения
        executorService.submit(() -> handleUpdate(update));
    }

    private void handleUpdate(Update update){
        if(update.hasCallbackQuery()){
            hasCallbackQuery(update);
        }else if(update.getMessage().hasDocument()){
            hasDocument(update);
        }else{
            hasText(update);
        }
    }

    private void hasCallbackQuery(Update update){
        //getCallBackQuery дает те же возможности что и message, но получить message можно только из CallBackQuery.getMessage
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        //удаление статуса создания папки
        if(UserRepository.getUser(chatId) && UserRepository.getCanAddFolder(chatId)) {
            UserRepository.setCanAddFolder(chatId, 1);
        }
        String data = update.getCallbackQuery().getData();
        System.out.println("User pressed button. User's chatId: " + chatId + " | CallbackQuery is: " + data);
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
            int messageId = update.getCallbackQuery().getMessage().getMessageId();
            try {
                DeleteMessage deleteMessage = Messages.deleteMessage(chatId, messageId);
                //устанавливаем удаляемое сообщение
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
                throw new RuntimeException(e);
            }
        }else{
            System.out.println("try");
            try {
                long messageId = update.getCallbackQuery().getMessage().getMessageId();
                sendEditMessageResponse(chatId, data, messageId);
            } catch (IOException | TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void hasDocument(Update update){
        long chatId = update.getMessage().getChatId();
        //проверка есть ли пользователь в бд
        System.out.println("User sends DOCUMENT. Checking user in database with chatId: " + chatId);
        if(!UserRepository.getUser(chatId)){
            System.out.println("No USER in database with chatId. Adding USER to database with chatId: " + chatId);
            //добавляем пользователя в бд, если его нет
            String name = update.getMessage().getChat().getUserName();
            UserRepository.addUser(chatId);
            UserRepository.setName(chatId, name);
        }
        //удаление статуса создания папки
        UserRepository.setCanAddFolder(chatId, 0);
        String selectedPath = UserRepository.getFilePath(chatId);
        if(!selectedPath.isEmpty()) {
            Document document = update.getMessage().getDocument();
            System.out.println("Save file from user with chatId: " + chatId);
            saveFile(document, chatId, update.getMessage().getCaption(), selectedPath);
        }else{
            SendMessage message = new SendMessage();
            try {
                Messages.sendMessage(message, FilesAndFolders.getFilesFromFolder(path), chatId, "Сначала выберите папку куда будете сохранять");
                execute(message);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void hasText(Update update){
        System.out.println("User sends message with only text");
        if(update.getMessage().getText().equals("/start")){
            long chatId = update.getMessage().getChatId();
            System.out.println("Checking user in database with chatId: " + chatId);
            //проверка есть ли пользователь в бд
            if(!UserRepository.getUser(chatId)){
                System.out.println("No USER in database with chatId. Adding USER to database with chatId: " + chatId);
                //добавляем пользователя в бд, если его нет
                String name = update.getMessage().getChat().getUserName();
                UserRepository.addUser(chatId);
                UserRepository.setName(chatId, name);
                System.out.println("Adding user SUCCESSFUL");
            }
            try {
                sendNewMessageResponse(chatId, "/start");
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }else if(update.getMessage().hasText()){
            long chatId = update.getMessage().getChatId();
            String text = update.getMessage().getText();
            //проверка есть ли пользователь в бд
            System.out.println("User send message with text. Checking user in database with chatId: " + chatId);
            if(!UserRepository.getUser(chatId)){
                System.out.println("No USER in database with chatId. Adding USER to database with chatId: " + chatId);
                //добавляем пользователя в бд, если его нет
                String name = update.getMessage().getChat().getUserName();
                UserRepository.addUser(chatId);
                UserRepository.setName(chatId, name);
                System.out.println("Adding user SUCCESSFUL");
            }
            boolean canAddFolder = UserRepository.getCanAddFolder(chatId);
            if(canAddFolder){
                try {
                    sendNewMessageResponse(chatId, text);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void saveFile(Document document, long chatId, String text, String selectedPath){
        try {
            String fileId = document.getFileId();
            String filePath = execute(new GetFile(fileId)).getFilePath();
            //обращение к TelegramAPI для получения инфы о файле
            String fullFilePath = "https://api.telegram.org/file/bot" + bot_token + "/" + filePath;
            //открываем поток для чтения
            InputStream is = new URL(fullFilePath).openStream();
            String fileName = document.getFileName();
            System.out.println(fileName + "filename");
            //получаем расширение файла
            String extension = fileName.substring(fileName.lastIndexOf("."));
            if(text != null) {
                System.out.println(1);
                Files.copy(is, Paths.get(selectedPath + "\\" + text +  extension));
            }else{
                System.out.println(2);
                Files.copy(is, Paths.get(selectedPath + "\\" + fileName));
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
            case "/sqlError":
                Messages.sendMessage(message, Messages.setMainMenuButtons(), chatid, "Ошибка! Пожалуйста напиши боту /start");
                execute(message);
                break;
            default:
                try {
                    FilesAndFolders.addFolder(data);
                    Messages.sendMessage(message, Messages.setMainMenuButtons(), chatid, "Директория создана");
                    execute(message);
                    UserRepository.setCanAddFolder(chatid, 0);
                }catch (IOException e){
                    sendNewMessageResponse(chatid, "/error");
                    System.out.println(e);
                }
                break;
        }
    }

    public boolean sendEditMessageResponse(long chatId, String data, long messageID) throws IOException, TelegramApiException {
        //метод для ИЗМЕНЕНИЯ существующего сообщения / метод возвращает bool чтобы остановить выполнение когда нужно
        EditMessageText message = new EditMessageText();
        message.setMessageId((int) messageID);
        switch (data) {
            case "LessonButtonPressed":
                System.out.println("LessonPressed");
                Messages.editMessage(message, Messages.setLessonMenuButtons(), chatId, "Выберите дату");
                execute(message);
                return true;
            case "TodayLessonsButtonPressed":
                //проверка пользователя в бд
                if(!UserRepository.getUser(chatId)){
                    UserRepository.addUser(chatId);
                    Messages.editMessage(message, Messages.setMainMenuButtons(), chatId, "Выберите группу, чтобы получить расписание");
                    execute(message);
                    return false;
                }
                String obj = UserRepository.getObj(chatId);
                //если юзер не выбрал группу, то просим его это сделать
                if(obj.equals("Not found") || obj == null || obj.isEmpty()){
                    Messages.editMessage(message, Messages.setMainMenuButtons(), chatId, "Выберите группу, чтобы получить расписание");
                    execute(message);
                    return false;
                }
                //пытаемся вернуть значение из кэша
                if(lessonsCache.containsKey(obj) && !lessonsCache.get(obj).isExpired()){
                    System.out.println("Возвращение из кэша Today");
                    Messages.editMessage(message, Messages.setLessonMenuButtons(), chatId, lessonsCache.get(obj).lessonsToday);
                    execute(message);
                    return true;
                }
                //если значение из кэша является не актуальным
                getLessons(obj);
                Messages.editMessage(message, Messages.setLessonMenuButtons(), chatId, lessonsCache.get(obj).lessonsToday);
                execute(message);
                return true;
            case "TomorrowLessonsButtonPressed":
                //проверка пользователя в бд
                if(!UserRepository.getUser(chatId)){
                    UserRepository.addUser(chatId);
                    Messages.editMessage(message, Messages.setMainMenuButtons(), chatId, "Выберите группу, чтобы получить расписание");
                    execute(message);
                    return false;
                }
                String obj1 = UserRepository.getObj(chatId);
                //если юзер не выбрал группу, то просим его это сделать
                if(obj1.equals("Not found") || obj1 == null || obj1.isEmpty()){
                    Messages.editMessage(message, Messages.setMainMenuButtons(), chatId, "Выберите группу, чтобы получить расписание");
                    execute(message);
                    return false;
                }
                //пытаемся вернуть значение из кэша
                if(lessonsCache.containsKey(obj1) && !lessonsCache.get(obj1).isExpired()){
                    System.out.println("Возвращение из кэша Today");
                    Messages.editMessage(message, Messages.setLessonMenuButtons(), chatId, lessonsCache.get(obj1).lessonsTomorrow);
                    execute(message);
                    return true;
                }
                //если значение из кэша является не актуальным
                getLessons(obj1);
                Messages.editMessage(message, Messages.setLessonMenuButtons(), chatId, lessonsCache.get(obj1).lessonsTomorrow);
                execute(message);
                return true;
            case "BackButtonPressed":
                Messages.editMessage(message, Messages.setMainMenuButtons(), chatId, "Выберите функцию");
                execute(message);
                return true;
            case "FileButtonPressed":
                Messages.editMessage(message, FilesAndFolders.getFilesFromFolder(path), chatId, "Выберите вашу папку вашей группы");
                execute(message);
                return true;
            case "AddFolderButtonPressed":
                Messages.editMessage(message, Messages.setMainMenuButtons(), chatId, "Напишите название директории \n пишите без пробелов и спец.символов");
                execute(message);
                UserRepository.setCanAddFolder(chatId, 1);
                return true;
            case "selectYearButtonPressed":
                System.out.println("User pressed selectYearButton. User's Id is: " + chatId);
                if(!yearsAndGroupsCache.containsKey("Year")){
                    Messages.setSelectYearButtons();
                }
                Messages.editMessage(message, yearsAndGroupsCache.get("Year"), chatId, "Выберите курс");
                execute(message);
                return true;
        }

        //если нажали на кнопку директории
        if(data.contains("Folder")){
            //путь директории
            String path1 = data.replace("Folder", "");
            System.out.println(path1);
            Messages.editMessage(message, FilesAndFolders.getFilesFromFolder(path1), chatId, "Выберите файл или загрузите его");
            execute(message);
            if(UserRepository.getUser(chatId)){
                UserRepository.setFilePath(chatId, path1);
            }else{
                UserRepository.addUser(chatId);
                UserRepository.setFilePath(chatId, path1);
                return false;
            }
            return true;
        }else if(data.contains("Year")){
            int index = data.indexOf("Year");
            //получаем индекс начала Year
            String num = data.substring(index);
            //создаем строку к коорой привяжем значение obj
            num = num.replace("Year", "");
            //удаляем не нужное
            int number = Integer.parseInt(num);
            if(!yearsAndGroupsCache.containsKey("Group"+number)){
                //получаем группы и сохраняем в yearsAndGroupsCache
                Messages.setGroupSelectButtons(number);
            }
            Messages.editMessage(message, yearsAndGroupsCache.get("Groups"+number), chatId, "Выберите группу");
            execute(message);
            return true;
        }else if(data.contains("Group")){
            //получаем индекс начала Group
            int index = data.indexOf("Group");
            //получаем obj группы
            String group = data.substring(index);
            group = group.replace("Group=", "");
            if(!UserRepository.getUser(chatId)){
                Messages.editMessage(message, Messages.setMainMenuButtons(), chatId, "Ошибка! Напишите /start для продолжения");
                execute(message);
            }else{
                System.out.println("add group");
                if(!UserRepository.getUser(chatId)){
                    UserRepository.addUser(chatId);
                    Messages.editMessage(message, Messages.setMainMenuButtons(), chatId, "Выберите группу, чтобы получить расписание");
                    execute(message);
                    return false;
                }
                UserRepository.setObj(chatId, group);
            }
            Messages.editMessage(message, Messages.setMainMenuButtons(), chatId, "Группа сохранена");
            execute(message);
            return true;
        }
        return false;
    }

    //получение расписания на сегодня
    private boolean getLessons(String obj) throws IOException {
        if(lessonsCache.containsKey(obj) && lessonsCache.get(obj).isExpired()){
            lessonsCache.remove(obj);
        }
        System.out.println("Obj ======================= " + obj);
        LocalDate localDate = LocalDate.now();
        String today = localDate.format(dateTimeFormatter);
        LocalDate localDateTomorrow = LocalDate.now().plusDays(1);
        String tomorrow = localDateTomorrow.format(dateTimeFormatter);
        String lessonsToday = ParseSite.getDay(today, obj);
        String lessonsTomorrow = ParseSite.getDay(tomorrow, obj);
        lessonsCache.put(obj, new CachedLessons(lessonsToday, lessonsTomorrow, Long.parseLong(duration)));
        System.out.println("Сохранение в кэш: " + lessonsCache.get(obj).toString());
        return true;
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
        String lessonsToday;
        String lessonsTomorrow;
        long timestamp;
        long duration;

        public CachedLessons(String lessonsToday, String lessonsTomorrow, long duration){
            this.timestamp = System.currentTimeMillis();
            this.lessonsTomorrow = lessonsTomorrow;
            this.lessonsToday = lessonsToday;
            this.duration = duration;
        }

        public boolean isExpired(){
            //текущее время - время создания > времени существования
            return System.currentTimeMillis() - timestamp > TimeUnit.MINUTES.toMillis(duration);
        }
    }
}