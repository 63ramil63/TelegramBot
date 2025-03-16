package org.example.Files;

import org.example.bot.TelegramBot;
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


public class FilesAndFolders {
    public static InlineKeyboardMarkup getFilesFromFolder(String path){
        System.out.println("path: " + path);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        //поиск файлов из указанной директории
        try(DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(path))){
            stream.forEach(path1 -> {
                List<InlineKeyboardButton> row = new ArrayList<>();
                //удаление пути из названия файлов
                String fileName = path1.toString().replace(path, "");
                System.out.println("fileName: " + fileName);
                if(!Files.isDirectory(path1)){
                    //удаление из навзания лишней '\'
                    row.add(TelegramBot.setButton(fileName.replace("\\", ""), path1 +  "File"));
                }else{
                    row.add(TelegramBot.setButton(fileName, path1 + "Folder"));
                }
                keyboard.add(row);
            });

            //добавление кнопки назад
            List<InlineKeyboardButton> row = new ArrayList<>();

            if(path.equals(TelegramBot.path)){
                row.add(TelegramBot.setButton("Добавить папку", "AddFolderButtonPressed"));
            }


            row.add(TelegramBot.setButton("Назад", "BackButtonPressed"));
            keyboard.add(row);


            markup.setKeyboard(keyboard);
        }catch (IOException e){
            System.out.println(e);
        }
        return markup;
    }

    public static SendDocument sendMessageWithDoc(String fileName, long chatId){
        //File$ - регулярное выражение которое удаляет одно вхождение с конца
        String correctFileName = fileName.replaceAll("File$", "");
        SendDocument message = new SendDocument();
        message.setChatId((Long) chatId);
        message.setDocument(new InputFile(new File(correctFileName)));
        return message;
    }


    public static void addFolder(String text) throws IOException {
        if(!Files.isDirectory(Path.of(TelegramBot.path + text))){
            Path directory = Files.createDirectory(Path.of(TelegramBot.path + text));
        }
    }
}
