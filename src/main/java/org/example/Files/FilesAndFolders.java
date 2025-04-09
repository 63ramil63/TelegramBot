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
import java.util.List;
import java.util.concurrent.ExecutorService;


public class FilesAndFolders {
    private final ExecutorService executorService;

    public FilesAndFolders(ExecutorService executorService){
        this.executorService = executorService;
    }

    public InlineKeyboardMarkup getFilesFromFolder(String path){
        System.out.println("Get files/folders from: " + path);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        //поиск файлов из указанной директории
        try(DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(path))){
            stream.forEach(path1 -> {
                List<InlineKeyboardButton> row = new ArrayList<>();
                //удаление пути из названия файлов
                String fileName = path1.toString().replace(path, "");
                if(!Files.isDirectory(path1)){
                    //удаление из названия лишней '\', добавляем File чтобы можно было определить что мы нажимаем именно на файл
                    row.add(Messages.setButton(fileName.replace("\\", ""), path1 +  "File"));
                }else{
                    //добавляем Folder, чтобы можно было определить, что это директория
                    row.add(Messages.setButton(fileName, path1 + "Folder"));
                }
                keyboard.add(row);
            });
            //добавление кнопки назад
            List<InlineKeyboardButton> row = new ArrayList<>();
            if(path.equals(TelegramBot.path)){
                row.add(Messages.setButton("Добавить папку", "AddFolderButtonPressed"));
            }
            row.add(Messages.setButton("Назад", "BackButtonPressed"));
            keyboard.add(row);
            markup.setKeyboard(keyboard);
        }catch (IOException e){
            System.out.println(e);
        }
        return markup;
    }

    public void sendMessageWithDocAsync(SendDocument message ,String fileName, long chatId){
        executorService.submit(() -> sendMessageWithDoc(message, fileName, chatId));
    }

    public void sendMessageWithDoc(SendDocument message ,String fileName, long chatId){
        System.out.println("Send document to user with chatID: " + chatId + "\n Document name is: " + fileName);
        //File$ - регулярное выражение которое удаляет одно вхождение с конца
        String correctFileName = fileName.replaceAll("File$", "");
        message.setChatId((Long) chatId);
        message.setDocument(new InputFile(new File(correctFileName)));
    }

    //асинхронное добавление файлов
    public void addFolderAsync(String text){
        executorService.submit(() -> {
            try {
                addFolder(text);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void addFolder(String text) throws IOException {
        if(!Files.isDirectory(Path.of(TelegramBot.path  + text))){
            Files.createDirectory(Path.of(TelegramBot.path + text));
        }
    }
}
