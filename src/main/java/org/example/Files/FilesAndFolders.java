package org.example.Files;

import org.example.bot.TelegramBot;
import org.example.messages.Messages;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class FilesAndFolders {
    private final ExecutorService executorService;

    public FilesAndFolders() {
        executorService = Executors.newFixedThreadPool(10);
        Runtime.getRuntime().addShutdownHook(new Thread(executorService::close));
    }

    /**
     * method finds and sets row with the button defined like button or directory
     * @param path1 path to file/directory
     * @return row with button that can be defined as a file or directory
     */
    private List<InlineKeyboardButton> setRow(Path path1) {
        List<InlineKeyboardButton> row = new ArrayList<>();
        //удаление пути из названия файлов
        String fileName = path1.toString().replace(TelegramBot.path, "");

        //проверяем, является ли путь директорией
        if (!Files.isDirectory(path1)) {
            //удаление из названия лишней '\', добавляем File чтобы можно было определить что это именно на файл
            int index = fileName.indexOf(TelegramBot.delimiter);
            String buttonName = fileName.substring(index).replace(TelegramBot.delimiter, "");
            row.add(Messages.setButton(buttonName, fileName + "File"));
        } else {
            //добавляем Folder, чтобы можно было определить, что это директория
            row.add(Messages.setButton(fileName, fileName + "Folder"));
        }
        return row;
    }


    public List<InlineKeyboardButton> setRow(InlineKeyboardButton ... buttons) {
        return new ArrayList<>(Arrays.asList(buttons));
    }

    /**
     *
     * @param path where method will find files/folders
     * @return InlineKeyboardMarkup with buttons of file/folders in path
     */
    public InlineKeyboardMarkup getFilesFromFolder(String path) {
        System.out.println("Get files and folders from: " + path);

        //создаем новую клавиатуру и markup к нему
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        //если указанный путь не равен изначальному пути, то добавляем в начало путь, указанный в TelegramBot.path
        if (!path.equals(TelegramBot.path)) {
            path = TelegramBot.path + path;
        }

        //поиск файлов из указанной директории
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(path))) {
            for (Path path1: stream) {
                keyboard.add(setRow(path1));
            }

            InlineKeyboardButton back = Messages.setButton("Назад", "BackButtonPressed");
            //добавление различных кнопок
            if (path.equals(TelegramBot.path)) {
                InlineKeyboardButton addFolder =  Messages.setButton("Добавить папку", "AddFolderButtonPressed");
                keyboard.add(setRow(addFolder, back));
            } else {
                keyboard.add(setRow(back));
            }
            markup.setKeyboard(keyboard);
        } catch (IOException e) {
            System.out.println(e);
            throw new RuntimeException(e);
        }
        return markup;
    }

    public void sendMessageWithDoc(SendDocument message, String fileName, long chatId) {
        System.out.println("Send document to user with chatID: " + chatId + "\n Document name is: " + fileName);
        //File$ - регулярное выражение которое удаляет одно вхождение с конца
        String correctFileName = fileName.replaceAll("File$", "");
        message.setChatId(chatId);
        message.setDocument(new InputFile(new File(TelegramBot.path + correctFileName)));
    }

    //асинхронное добавление директорий
    public void addFolderAsync(String text) {
        executorService.submit(() -> {
            try {
                addFolder(text);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void addFolder(String text) throws IOException {
        if (!Files.isDirectory(Path.of(TelegramBot.path + text))) {
            Files.createDirectory(Path.of(TelegramBot.path + text));
        }
    }
}
