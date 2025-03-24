package org.example;

import org.example.bot.TelegramBot;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main{
    private static String URL;
    private static String PASS;
    private static String USER;
    public static void main(String[] args) throws TelegramApiException, IOException {
        TelegramBotsApi tgBot = new TelegramBotsApi(DefaultBotSession.class);
        tgBot.registerBot(new TelegramBot());
        Properties properties = new Properties();
        try(FileInputStream fis = new FileInputStream("src/main/resources/config.properties")){
            properties.load(fis);
            URL = properties.getProperty("databaseURL");
            USER = properties.getProperty("user");
            PASS = properties.getProperty("pass");
        }catch (IOException e){
            throw new RuntimeException(e);
        }

    }
}