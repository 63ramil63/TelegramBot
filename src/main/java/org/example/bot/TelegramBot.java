package org.example.bot;

import org.example.Files.FilesAndFolders;
import org.example.Main;
import org.example.ParseSite;
import org.example.database.UserRepository;
import org.example.messages.Messages;
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
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class TelegramBot extends TelegramLongPollingBot {
    final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final HashMap<String, CachedLessons> lessonsCache = new HashMap<>();
    public static HashMap<String, InlineKeyboardMarkup> yearsAndGroupsCache = new HashMap<>();
    private FilesAndFolders filesAndFolders;

    private String bot_token;
    private String bot_name;
    private String duration;
    private List<String> allowedExtensions;
    public static String path;
    public static int maxFileSize;
    public static String delimiter;

    //newCachedThreadPool - создает пул потоков, который может создавать новые потоки по мере необходимости, но при этом повторно использовать ранее созданные потоки, если они свободны
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    public TelegramBot() {
        loadConfig();
    }

    //загрузка данных из файла конфига
    public void loadConfig() {
        Properties properties = new Properties();
        filesAndFolders = new FilesAndFolders();
        try (FileInputStream fileInputStream = new FileInputStream(Main.propertyPath)) {
            properties.load(fileInputStream);
            bot_token = properties.getProperty("bot_token");
            bot_name = properties.getProperty("bot_name");
            //время обновления расписания в минутах
            duration = properties.getProperty("duration");
            //путь, где хранятся файлы
            path = properties.getProperty("path");
            //указываем разделитель '\\ для win' | '/ для Linux'
            delimiter = properties.getProperty("delimiter");
            //допустимые расширения файлов
            allowedExtensions = List.of(properties.getProperty("extensions").split(","));
            //макс размер файла в мб
            maxFileSize = Integer.parseInt(properties.getProperty("fileMaxSize"));
        } catch (IOException e) {
            System.out.println(e);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(executorService::close));
    }

    @Override
    public void onUpdateReceived(Update update) {
        //создаем/используем поток при получении сообщения
        executorService.submit(() -> handleUpdate(update));
    }

    //проверяем ответ пользователя
    private void handleUpdate(Update update) {
        if (update.hasCallbackQuery()) {
            hasCallbackQuery(update);
        } else if (update.getMessage().hasDocument()) {
            hasDocument(update);
        } else {
            hasText(update);
        }
    }

    private void checkAndAddUser(long chatId) {
        if (!UserRepository.getUser(chatId)) {
            UserRepository.addUser(chatId);
        }
    }

    private void checkCallBackData(long chatId, String data, int messageId) {
        if (data.contains("Folder") || data.equals("FileButtonPressed")) {
            try {
                sendEditMessageResponse(chatId, data, messageId);
            } catch (IOException | TelegramApiException e) {
                throw new RuntimeException(e);
            }
        } else if (data.contains("File")) {
            try {
                //устанавливаем и удаляем сообщение
                DeleteMessage deleteMessage = new DeleteMessage();
                Messages.deleteMessage(deleteMessage, chatId, messageId);
                execute(deleteMessage);

                //подготовка документа
                SendDocument sendDocument = new SendDocument();
                filesAndFolders.sendMessageWithDoc(sendDocument, data, chatId);

                //Отправка (асинхронно)
                executorService.submit(() -> {
                    try {
                        execute(sendDocument);
                        sendNewMessageResponse(chatId, "/start");
                    } catch (TelegramApiException e) {
                        //отправка сообщения об ошибке
                        try {
                            sendNewMessageResponse(chatId, "/error");
                        } catch (TelegramApiException ex) {
                            throw new RuntimeException(ex);
                        }
                        throw new RuntimeException(e);
                    }
                });

            } catch (TelegramApiException e) {
                //отправка сообщения об ошибке
                try {
                    sendNewMessageResponse(chatId, "/error");
                } catch (TelegramApiException ex) {
                    throw new RuntimeException(ex);
                }
                throw new RuntimeException(e);
            }

        } else {
            try {
                sendEditMessageResponse(chatId, data, messageId);
            } catch (IOException | TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void hasCallbackQuery(Update update) {
        //getCallBackQuery дает те же возможности, что и message, но получить message можно только из CallBackQuery.getMessage
        long chatId = update.getCallbackQuery().getMessage().getChatId();

        //проверка пользователя
        checkAndAddUser(chatId);

        //удаление статуса создания папки
        UserRepository.setCanAddFolder(chatId, (byte) 0);

        //получаем информацию о нажатой кнопке
        String data = update.getCallbackQuery().getData();
        System.out.println("User pressed button. User's chatId: " + chatId + " | CallbackQuery is: " + data);

        int messageId = update.getCallbackQuery().getMessage().getMessageId();
        checkCallBackData(chatId, data, messageId);
    }

    private void hasDocument(Update update) {
        long chatId = update.getMessage().getChatId();
        //проверка есть ли пользователь в бд
        System.out.println("User sends DOCUMENT. Checking user in database with chatId: " + chatId);

        //проверяем пользователя в бд
        checkAndAddUser(chatId);

        //удаление статуса создания папки
        UserRepository.setCanAddFolder(chatId, (byte) 0);

        //получаем путь к папке, которую выбрал пользователь
        String selectedPath = UserRepository.getFilePath(chatId);

        //проверяем не пусто ли значение выбранной папки пользователя
        if (!selectedPath.isEmpty()) {
            //сохраняем файл
            Document document = update.getMessage().getDocument();
            System.out.println("Save file from user with chatId: " + chatId);
            saveFileAsync(document, chatId, update.getMessage().getCaption(), selectedPath);
        } else {
            //пишем чтобы пользователь выбрал путь
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
            getMessageStart(update);
        } else if (update.getMessage().hasText()) {
            System.out.println("another");
            getMessageWithAnyText(update);
        }
    }

    private void getMessageStart(Update update){
        System.out.println("getMessageStart");
        //получаем инфу о пользователе
        long chatId = update.getMessage().getChatId();


        //проверка есть ли пользователь в бд
        if (!UserRepository.getUser(chatId)) {
            String userName = update.getMessage().getChat().getUserName();
            String firstName = update.getMessage().getChat().getFirstName();
            String lastName = update.getMessage().getChat().getLastName();

            //добавляем пользователя в бд, если его нет
            UserRepository.addUser(chatId);
            UserRepository.setUserName(chatId, userName);
            UserRepository.setUserFullName(chatId, firstName + " " + lastName);
            System.out.println("Adding user SUCCESSFUL. User's chatId is " + chatId);
        }

        //присылаем ответ пользователю
        try {
            sendNewMessageResponse(chatId, "/start");
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void getMessageWithAnyText(Update update){
        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();

        //проверка есть ли пользователь в бд
        checkAndAddUser(chatId);

        //проверяем, хочет ли пользователь добавить новую папку
        boolean canAddFolder = UserRepository.getCanAddFolder(chatId);
        if (canAddFolder) {
            try {
                sendNewMessageResponse(chatId, text);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void saveFileAsync(Document document, long chatId, String text, String selectedPath){
        executorService.submit(() -> saveFile(document, chatId, text, selectedPath));
    }

    private void saveFile(Document document, long chatId, String text, String selectedPath) {
        try {
            if(document.getFileSize() > (long) maxFileSize * 1024 * 1024){
                sendNewMessageResponse(chatId, "/fileTooLarge");
                return;
            }

            //получаем инфу о файле
            String fileId = document.getFileId();
            String filePath = execute(new GetFile(fileId)).getFilePath();
            //обращение к TelegramAPI для получения инфы о файле
            String fullFilePath = "https://api.telegram.org/file/bot" + bot_token + "/" + filePath;
            //открываем поток для чтения
            InputStream is = new URL(fullFilePath).openStream();
            String fileName = document.getFileName();
            System.out.println("User sends DOCUMENT. User id is " + chatId + " - filename is: " + fileName);
            //получаем расширение файла(используем +1, чтобы получить расширение файла без точки)
            String extension = fileName.substring(fileName.lastIndexOf(".") + 1);

            //проверяем расширение файла
            if (allowedExtensions.contains(extension)) {
                //если пользователь отправил файл с описанием, то записываем описание в название файла
                //копируем файл из потока в путь
                if (text != null) {
                    Files.copy(is, Paths.get(selectedPath + delimiter + text + '.' + extension));
                } else {
                    Files.copy(is, Paths.get(selectedPath + delimiter + fileName));
                }
                is.close();
                sendNewMessageResponse(chatId, "/fileSaved");
            }else {
                sendNewMessageResponse(chatId, "/extensionErr");
            }
        } catch (TelegramApiException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendNewMessageResponse(long chatid, String data) throws TelegramApiException {
        //метод для ОТПРАВКИ сообщения
        SendMessage message = new SendMessage();
        message.setChatId((Long) chatid);
        switch (data) {
            case "/fileSaved":
                Messages.sendMessage(message, chatid, "Файл сохранён");
                execute(message);
                //отправка сообщения без кнопки
                sendNewMessageResponse(chatid, "/start");
                //отправка сообщения с кнопками
                break;

            case "/fileTooLarge":
                Messages.sendMessage(message, chatid, "Файл слишком большой");
                execute(message);
                //отправка сообщения без кнопки
                sendNewMessageResponse(chatid, "/start");
                //отправка сообщения с кнопками
                break;

            case "/extensionErr":
                Messages.sendMessage(message, chatid, "Недопустимое расширение файла");
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
                //добавляем папку
                filesAndFolders.addFolderAsync(data);
                Messages.sendMessage(message, Messages.setMainMenuButtons(), chatid, "Директория создана");
                execute(message);
                UserRepository.setCanAddFolder(chatid, (byte) 0);
                break;
        }
    }

    private boolean hasObj(EditMessageText message, long chatId, long messageID, String obj) throws TelegramApiException, IOException {
        if (obj.equals("Not found")) {
            if (!yearsAndGroupsCache.containsKey("Year")) {
                sendEditMessageResponse(chatId, "SelectYearButtonPressed", messageID);
            } else {
                //если юзер не выбрал группу, то просим его это сделать
                Messages.editMessage(message, yearsAndGroupsCache.get("Year"), chatId, "Выберите группу, чтобы получить расписание");
            }
            execute(message);
            return false;
        }
        return true;
    }



    public void sendEditMessageResponse(long chatId, String data, long messageID) throws IOException, TelegramApiException {
        //метод для ИЗМЕНЕНИЯ существующего сообщения
        EditMessageText message = new EditMessageText();
        message.setMessageId((int) messageID);
        checkAndAddUser(chatId);

        switch (data) {
            case "/help":
                Messages.editMessage(message, Messages.setMainMenuButtons(), chatId, "Напишите /start, если что-то сломалось \n" +
                        "Чтобы сохранить файл, выберите путь и скиньте файл боту" +
                        "Старайтесь не делать названия файлов и директории слишком большими, бот может дать сбой \n" +
                        "Если столкнулись с проблемой, напишите в личку @wrotoftanks63");
                execute(message);
                return;

            case "LessonButtonPressed":
                Messages.editMessage(message, Messages.setLessonMenuButtons(), chatId, "Выберите дату");
                execute(message);
                return;

            case "TodayLessonsButtonPressed":
                String obj = UserRepository.getObj(chatId);
                //проверка, есть ли у пользователя значение obj в бд
                if(hasObj(message, chatId, messageID, obj)) {
                    getCachedLessons(message, chatId, obj, true);
                } else {
                    return;
                }
                execute(message);
                return;

            case "TomorrowLessonsButtonPressed":
                String obj1 = UserRepository.getObj(chatId);
                //проверка, есть ли у пользователя значение obj в бд
                if(hasObj(message, chatId, messageID, obj1)) {
                    getCachedLessons(message, chatId, obj1, false);
                } else {
                    return;
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

            //отправляем сообщение с файлами из директории
            Messages.editMessage(message, filesAndFolders.getFilesFromFolder(path1), chatId, "Выберите файл или загрузите его");
            execute(message);

            //исправляем путь к директории
            String correctPath = path + path1;

            //устанавливаем путь к директории
            UserRepository.setFilePath(chatId, correctPath);
        } else if (data.contains("Year")) {
            //получаем индекс начала Year
            int index = data.indexOf("Year");
            //создаем строку к которой привяжем значение obj
            String num = data.substring(index);
            //удаляем не нужное
            num = num.replace("Year", "");
            int number = Integer.parseInt(num);

            //проверяем есть ли в HashMap нкжные нам значения
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
            //убираем лишнюю строку
            group = group.replace("Group=", "");

            //устанавливаем значение setObj
            UserRepository.setObj(chatId, group);

            Messages.editMessage(message, Messages.setLessonMenuButtons(), chatId, "Группа сохранена");
            execute(message);
        }
    }

    //проверяем расписание, сохраненное в lessonCache
    private void getCachedLessons(EditMessageText message, long chatId, String obj, boolean today) throws TelegramApiException, IOException{
        //проверяем актуальность расписания
        if (!(lessonsCache.containsKey(obj) && !lessonsCache.get(obj).isExpired())) {
            //парсим расписание с сайта
            getLessons(obj);
        }
        //получаем расписание либо на сегодня, либо на завтра
        if (today) {
            Messages.editMessage(message, Messages.setLessonMenuButtons(), chatId, lessonsCache.get(obj).lessonsToday);
        } else {
            Messages.editMessage(message, Messages.setLessonMenuButtons(), chatId, lessonsCache.get(obj).lessonsTomorrow);
        }
    }


    //получение расписания на сегодня
    private boolean getLessons(String obj) throws IOException {
        //проверяем есть ли в кэше значение
        if (lessonsCache.containsKey(obj) && lessonsCache.get(obj).isExpired()) {
            lessonsCache.remove(obj);
        }

        //получаем и форматируем дату на сегодняшнюю дату и завтрашнюю
        LocalDate localDate = LocalDate.now();
        String today = localDate.format(dateTimeFormatter);
        LocalDate localDateTomorrow = LocalDate.now().plusDays(1);
        String tomorrow = localDateTomorrow.format(dateTimeFormatter);
        String lessonsToday = ParseSite.getDay(today, obj);
        String lessonsTomorrow = ParseSite.getDay(tomorrow, obj);

        //устанавливаем значения для lessonsCache
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