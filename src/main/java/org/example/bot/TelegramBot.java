package org.example.bot;

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
import java.util.HashMap;
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
    private FilesAndFolders filesAndFolders;
    //newCachedThreadPool - создает пул потоков, который может создавать новые потоки по мере необходимости, но при этом повторно использовать ранее созданные потоки, если они свободны
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public TelegramBot() {
        loadConfig();
    }

    //загрузка данных из файла конфига
    public void loadConfig() {
        Properties properties = new Properties();
        filesAndFolders = new FilesAndFolders(executorService);
        try (FileInputStream fileInputStream = new FileInputStream(Main.propertyPath)) {
            properties.load(fileInputStream);
            bot_token = properties.getProperty("bot_token");
            bot_name = properties.getProperty("bot_name");
            duration = properties.getProperty("duration");
            path = properties.getProperty("path");
        } catch (IOException e) {
            log.error("e: ", e);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            executorService.close();
        }));
    }

    @Override
    public void onUpdateReceived(Update update) {
        //создаем/используем поток при получении сообщения
        executorService.submit(() -> handleUpdate(update));
    }

    private void handleUpdate(Update update) {
        if (update.hasCallbackQuery()) {
            hasCallbackQuery(update);
        } else if (update.getMessage().hasDocument()) {
            hasDocument(update);
        } else {
            hasText(update);
        }
    }

    private void hasCallbackQuery(Update update) {
        //getCallBackQuery дает те же возможности что и message, но получить message можно только из CallBackQuery.getMessage
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        //удаление статуса создания папки
        if (UserRepository.getUser(chatId) && UserRepository.getCanAddFolder(chatId)) {
            UserRepository.setCanAddFolder(chatId, (byte) 0);
        }
        String data = update.getCallbackQuery().getData();
        System.out.println("User pressed button. User's chatId: " + chatId + " | CallbackQuery is: " + data);
        if (data.contains("Folder") || data.equals("FileButtonPressed")) {
            try {
                long messageId = update.getCallbackQuery().getMessage().getMessageId();
                sendEditMessageResponse(chatId, data, messageId);
            } catch (IOException | TelegramApiException e) {
                throw new RuntimeException(e);
            }
        } else if (data.contains("File")) {
            int messageId = update.getCallbackQuery().getMessage().getMessageId();
            try {
                //устанавливаем удаляемое сообщение
                DeleteMessage deleteMessage = Messages.deleteMessage(chatId, messageId);
                execute(deleteMessage);
                SendDocument sendDocument = new SendDocument();
                filesAndFolders.sendMessageWithDocAsync(sendDocument, data, chatId);
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
        } else {
            try {
                long messageId = update.getCallbackQuery().getMessage().getMessageId();
                sendEditMessageResponse(chatId, data, messageId);
            } catch (IOException | TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void hasDocument(Update update) {
        long chatId = update.getMessage().getChatId();
        //проверка есть ли пользователь в бд
        System.out.println("User sends DOCUMENT. Checking user in database with chatId: " + chatId);
        if (!UserRepository.getUser(chatId)) {
            System.out.println("No USER in database with chatId. Adding USER to database with chatId: " + chatId);
            //добавляем пользователя в бд, если его нет
            String name = update.getMessage().getChat().getUserName();
            UserRepository.addUser(chatId);
            UserRepository.setUserName(chatId, name);
        }
        //удаление статуса создания папки
        UserRepository.setCanAddFolder(chatId, (byte) 0);
        String selectedPath = UserRepository.getFilePath(chatId);
        if (!selectedPath.isEmpty()) {
            Document document = update.getMessage().getDocument();
            System.out.println("Save file from user with chatId: " + chatId);
            saveFile(document, chatId, update.getMessage().getCaption(), selectedPath);
        } else {
            SendMessage message = new SendMessage();
            try {
                Messages.sendMessage(message, filesAndFolders.getFilesFromFolder(path), chatId, "Сначала выберите папку куда будете сохранять");
                execute(message);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void hasText(Update update) {
        System.out.println("User sends message with only text");
        if (update.getMessage().getText().equals("/start")) {
            long chatId = update.getMessage().getChatId();
            String userName = update.getMessage().getChat().getUserName();
            String firstName = update.getMessage().getChat().getFirstName();
            String lastName = update.getMessage().getChat().getLastName();
            //проверка есть ли пользователь в бд
            if (!UserRepository.getUser(chatId)) {
                //добавляем пользователя в бд, если его нет
                UserRepository.addUser(chatId);
                UserRepository.setUserName(chatId, userName);
                UserRepository.setUserFullName(chatId, firstName + " " + lastName);
                System.out.println("Adding user SUCCESSFUL. User's chatId is " + chatId);
            }
            try {
                UserRepository.setUserName(chatId, userName);
                UserRepository.setUserFullName(chatId, firstName + " " + lastName);
                sendNewMessageResponse(chatId, "/start");
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        } else if (update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            String text = update.getMessage().getText();
            //проверка есть ли пользователь в бд
            if (!UserRepository.getUser(chatId)) {
                //добавляем пользователя в бд, если его нет
                String name = update.getMessage().getChat().getUserName();
                UserRepository.addUser(chatId);
                UserRepository.setUserName(chatId, name);
                System.out.println("Adding user SUCCESSFUL. User's chatId is " + chatId);
            }
            boolean canAddFolder = UserRepository.getCanAddFolder(chatId);
            if (canAddFolder) {
                try {
                    sendNewMessageResponse(chatId, text);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void saveFile(Document document, long chatId, String text, String selectedPath) {
        try {
            System.err.println("User sends DOCUMENT. User id is " + chatId);
            String fileId = document.getFileId();
            String filePath = execute(new GetFile(fileId)).getFilePath();
            //обращение к TelegramAPI для получения инфы о файле
            String fullFilePath = "https://api.telegram.org/file/bot" + bot_token + "/" + filePath;
            //открываем поток для чтения
            InputStream is = new URL(fullFilePath).openStream();
            String fileName = document.getFileName();
            System.out.println(fileName + " - filename");
            //получаем расширение файла
            String extension = fileName.substring(fileName.lastIndexOf("."));
            if (text != null) {
                Files.copy(is, Paths.get(selectedPath + "\\" + text + extension));
            } else {
                Files.copy(is, Paths.get(selectedPath + "\\" + fileName));
            }
            //копируем файл из потока в путь
            is.close();
            sendNewMessageResponse(chatId, "FileSaved");
        } catch (TelegramApiException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendNewMessageResponse(long chatid, String data) throws TelegramApiException {
        //метод для ОТПРАВКИ сообщения
        SendMessage message = new SendMessage();
        message.setChatId((Long) chatid);
        switch (data) {
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
                filesAndFolders.addFolderAsync(data);
                Messages.sendMessage(message, Messages.setMainMenuButtons(), chatid, "Директория создана");
                execute(message);
                UserRepository.setCanAddFolder(chatid, (byte) 0);
                break;
        }
    }

    public void sendEditMessageResponse(long chatId, String data, long messageID) throws IOException, TelegramApiException {
        //метод для ИЗМЕНЕНИЯ существующего сообщения / метод возвращает bool чтобы остановить выполнение когда нужно
        EditMessageText message = new EditMessageText();
        message.setMessageId((int) messageID);
        switch (data) {
            case "/help":
                Messages.editMessage(message, Messages.setMainMenuButtons(), chatId, "Напишите /start, если что-то сломалось \n" +
                        "Старайтесь не делать названия файлов и директории слишком большими, бот может дать сбой \n" +
                        "Если столкнулись с проблемой, напишите в личку @wrotoftanks63");
                execute(message);
                return;
            case "LessonButtonPressed":
                Messages.editMessage(message, Messages.setLessonMenuButtons(), chatId, "Выберите дату");
                execute(message);
                return;
            case "TodayLessonsButtonPressed":
                //проверка пользователя в бд
                if (!UserRepository.getUser(chatId)) {
                    UserRepository.addUser(chatId);
                }
                String obj = UserRepository.getObj(chatId);
                if (obj.equals("Not found")) {
                    if (!yearsAndGroupsCache.containsKey("Year")) {
                        sendEditMessageResponse(chatId, "SelectYearButtonPressed", messageID);
                    } else {
                        //если юзер не выбрал группу, то просим его это сделать
                        Messages.editMessage(message, yearsAndGroupsCache.get("Year"), chatId, "Выберите группу, чтобы получить расписание");
                    }
                    execute(message);
                    return;
                } else if (lessonsCache.containsKey(obj) && !lessonsCache.get(obj).isExpired()) {
                    //пытаемся вернуть значение из кэша
                    Messages.editMessage(message, Messages.setLessonMenuButtons(), chatId, lessonsCache.get(obj).lessonsToday);
                } else {
                    //если значение из кэша является не актуальным
                    getLessons(obj);
                    Messages.editMessage(message, Messages.setLessonMenuButtons(), chatId, lessonsCache.get(obj).lessonsToday);
                }
                execute(message);
                return;
            case "TomorrowLessonsButtonPressed":
                //проверка пользователя в бд
                if (!UserRepository.getUser(chatId)) {
                    UserRepository.addUser(chatId);
                }
                String obj1 = UserRepository.getObj(chatId);
                if (obj1.equals("Not found")) {
                    if (!yearsAndGroupsCache.containsKey("Year")) {
                        sendEditMessageResponse(chatId, "SelectYearButtonPressed", messageID);
                    } else {
                        //если юзер не выбрал группу, то просим его это сделать
                        Messages.editMessage(message, yearsAndGroupsCache.get("Year"), chatId, "Выберите группу, чтобы получить расписание");
                    }
                    execute(message);
                    return;
                }else if (lessonsCache.containsKey(obj1) && !lessonsCache.get(obj1).isExpired()) {
                    //пытаемся вернуть значение из кэша
                    Messages.editMessage(message, Messages.setLessonMenuButtons(), chatId, lessonsCache.get(obj1).lessonsTomorrow);
                } else {
                    //если значение из кэша является не актуальным
                    getLessons(obj1);
                    Messages.editMessage(message, Messages.setLessonMenuButtons(), chatId, lessonsCache.get(obj1).lessonsTomorrow);
                }
                execute(message);
                return;
            case "BackButtonPressed":
                Messages.editMessage(message, Messages.setMainMenuButtons(), chatId, "Выберите функцию");
                execute(message);
                return;
            case "FileButtonPressed":
                Messages.editMessage(message, filesAndFolders.getFilesFromFolder(path), chatId, "Выберите вашу папку вашей группы");
                execute(message);
                return;
            case "AddFolderButtonPressed":
                Messages.editMessage(message, Messages.setMainMenuButtons(), chatId, "Напишите название директории \n пишите без спец.символов");
                execute(message);
                UserRepository.setCanAddFolder(chatId, (byte) 1);
                return;
            case "SelectYearButtonPressed":
                if (!yearsAndGroupsCache.containsKey("Year")) {
                    Messages.setSelectYearButtons();
                }
                Messages.editMessage(message, yearsAndGroupsCache.get("Year"), chatId, "Выберите курс");
                execute(message);
                return;
        }
        //если нажали на кнопку директории
        if (data.contains("Folder")) {
            //путь директории, Folder$ удаляет 1 вхождение с конца
            String path1 = data.replaceAll("Folder$", "");
            Messages.editMessage(message, filesAndFolders.getFilesFromFolder(path1), chatId, "Выберите файл или загрузите его");
            execute(message);
            //исправляем путь к директории
            String correctPath = path + path1;
            if (UserRepository.getUser(chatId)) {
                UserRepository.setFilePath(chatId, correctPath);
            } else {
                UserRepository.addUser(chatId);
                UserRepository.setFilePath(chatId, correctPath);
            }
        } else if (data.contains("Year")) {
            int index = data.indexOf("Year");
            //получаем индекс начала Year
            String num = data.substring(index);
            //создаем строку к коорой привяжем значение obj
            num = num.replace("Year", "");
            //удаляем не нужное
            int number = Integer.parseInt(num);
            if (!yearsAndGroupsCache.containsKey("Group" + number)) {
                //получаем группы и сохраняем в yearsAndGroupsCache
                Messages.setGroupSelectButtons(number);
            }
            //получаем из кэша группы
            Messages.editMessage(message, yearsAndGroupsCache.get("Groups" + number), chatId, "Выберите группу");
            execute(message);
        } else if (data.contains("Group")) {
            //получаем индекс начала Group
            int index = data.indexOf("Group");
            //получаем obj группы
            String group = data.substring(index);
            group = group.replace("Group=", "");

            if (!UserRepository.getUser(chatId)) {
                UserRepository.addUser(chatId);
                UserRepository.setObj(chatId, group);
            } else {
                UserRepository.setObj(chatId, group);
            }
            Messages.editMessage(message, Messages.setLessonMenuButtons(), chatId, "Группа сохранена");
            execute(message);
        }
    }

    //получение расписания на сегодня
    private boolean getLessons(String obj) throws IOException {
        if (lessonsCache.containsKey(obj) && lessonsCache.get(obj).isExpired()) {
            lessonsCache.remove(obj);
        }
        LocalDate localDate = LocalDate.now();
        String today = localDate.format(dateTimeFormatter);
        LocalDate localDateTomorrow = LocalDate.now().plusDays(1);
        String tomorrow = localDateTomorrow.format(dateTimeFormatter);
        String lessonsToday = ParseSite.getDay(today, obj);
        String lessonsTomorrow = ParseSite.getDay(tomorrow, obj);
        lessonsCache.put(obj, new CachedLessons(lessonsToday, lessonsTomorrow, Long.parseLong(duration)));
        System.out.println("Lessons saved in cache to Obj = " + obj);
        return true;
    }

    @Override
    public String getBotUsername() {
        return bot_name;
    }

    @Override
    public String getBotToken() {
        return bot_token;
    }

    private static class CachedLessons {
        String lessonsToday;
        String lessonsTomorrow;
        long timestamp;
        long duration;

        public CachedLessons(String lessonsToday, String lessonsTomorrow, long duration) {
            this.timestamp = System.currentTimeMillis();
            this.lessonsTomorrow = lessonsTomorrow;
            this.lessonsToday = lessonsToday;
            this.duration = duration;
        }

        public boolean isExpired() {
            //текущее время - время создания > времени существования
            return System.currentTimeMillis() - timestamp > TimeUnit.MINUTES.toMillis(duration);
        }
    }
}