package org.example;

import org.example.bot.TelegramBot;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import javax.print.Doc;
import javax.swing.plaf.basic.BasicDesktopIconUI;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main{
    static Document doc;
    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi tgBot = new TelegramBotsApi(DefaultBotSession.class);
        tgBot.registerBot(new TelegramBot());
    }

    public static String getDay(String _day) throws IOException{

        doc = Jsoup.connect("https://lk.ks.psuti.ru/?mn=2&obj=141").userAgent("Chrome").get();
        int num = 1;
        while(num < 60){
            Elements day = doc.select("body > table:nth-child(5) > tbody > tr:nth-child(" + num + ")");
            num ++;
            if(day.text().contains(_day)){
                return _day + getLesson(num);
            }
        }
        return "nothing to return";
    }

    public static String getLesson(int num) {
        num ++;     //чтобы не выбирало ненужные поля
        Elements currentElement = doc.select("body > table:nth-child(5) > tbody > tr:nth-child(" + num + ")");
        String lessons = "";
        while (!currentElement.text().isEmpty()) {      //проверка на последний элемент расписания(который всегда пустой)
            currentElement = doc.select("body > table:nth-child(5) > tbody > tr:nth-child(" + num + ")");
            Elements _number = doc.select("body > table:nth-child(5) > tbody > tr:nth-child(" + num + ") > td:nth-child(1)");
            Elements _time = doc.select("body > table:nth-child(5) > tbody > tr:nth-child(" + num + ") > td:nth-child(2)");
            Elements _lesson = doc.select("body > table:nth-child(5) > tbody > tr:nth-child(" + num + ") > td:nth-child(4)");
            num++;
            lessons += "\n" + _number.text() + " " + _time.text() + _lesson.text();
        }
        return lessons;
    }

}