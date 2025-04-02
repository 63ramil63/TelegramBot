package org.example;

import org.example.bot.TelegramBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main{

    public static String propertyPath = "";

    public static void main(String[] args) throws TelegramApiException {
        propertyPath = args[0];
        TelegramBotsApi tgBot = new TelegramBotsApi(DefaultBotSession.class);
        tgBot.registerBot(new TelegramBot());
    }
}