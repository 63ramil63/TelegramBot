package org.example;

import org.example.bot.TelegramBot;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class ParseSite {
    private static Document doc;
    public static String url = "https://lk.ks.psuti.ru/?mn=2&obj=";

    public static String getDay(String _day, Long chatId) throws IOException {
        System.out.println("get Day");
        doc = Jsoup.connect(url + TelegramBot.siteObj.get(chatId)).userAgent("Chrome").get();
        int num = 1;

        while(num < 60){
            if(match(_day, num, doc)){
                //проверка на совпадение дат
                num++;
                String lessons = getLesson(num);
                return _day + lessons;
            }
            num++;
        }

        Elements week = doc.select("body > table:nth-child(4) > tbody > tr:nth-child(4) > td > table > tbody > tr > td:nth-child(8) > a");
        String wk = week.attr("href");
        //получаем неделю
        int index = wk.indexOf("wk");
        wk = wk.substring(index);
        //удаляем ненужное

        num = 1;

        while(num < 60){
            Document newDoc = Jsoup.connect(url+"&"+wk).userAgent("Chrome").get();
            if(match(_day, num, newDoc)){
                num++;
                String lessons = getLesson(num);
                return _day + lessons;
            }
            num++;
        }


        return "ошибка \n https://lk.ks.psuti.ru/?mn=2&obj=" + TelegramBot.siteObj.get(chatId);
    }

    public static boolean match(String _day, int num, Document doc){
        Elements day = doc.select("body > table:nth-child(5) > tbody > tr:nth-child(" + num + ")");
        return day.text().contains(_day);
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
            lessons += "\n" + _number.text() + ") " + _time.text() + " " + _lesson.text();
        }
        lessons = lessons.replaceAll("\\s+$", "");
        lessons = lessons.substring(0, lessons.length() - 1);
        lessons = lessons.replaceAll("Московское шоссе, 120", "");
        lessons = lessons.replaceAll(" Замена Свободное время на:", "");
        //удаление всех пробелов в конце строки, убираем лишний '(', убираем наименование места
        return lessons;
    }


    public static List<String> getYear() throws IOException {
        Document doc = Jsoup.connect("https://lk.ks.psuti.ru/?mn=2").userAgent("Chrome").get();
        List<String> years = new ArrayList<>();
        int i = 1;
        while(i != 11){
            String year = doc.select("body > table:nth-child(5) > tbody > tr:nth-child(6) > td:nth-child(" + i + ")").text();
            if(!year.isEmpty()){
                years.add(year + "Year" + i);
            }
            i++;
        }
        //получаем курсы и помещаем их в массив
        return years;
    }

    public static List<String> getGroups(int num) throws IOException {
        System.out.println("getGroups");
        Document doc = Jsoup.connect("https://lk.ks.psuti.ru/?mn=2").userAgent("Chrome").get();
        List<String> groups = new ArrayList<>();
        Elements elementsSize = doc.select("body > table:nth-child(5) > tbody > tr:nth-child(7) > td:nth-child(" + num + ") > table > tbody > tr:nth-child(1) > td > table > tbody > tr");
        for(int i = 1; i < elementsSize.size() + 1; i++){
            Elements element = elementsSize.select("tr:nth-child(" + i + ")");
            //получаем строку в столбце
            Elements obj = element.select("td > a");
            //получаем <a> с атрибутом href
            String attribute = obj.attr("href");
            //получаем значение из атрибута <a>
            int index = attribute.indexOf("obj");
            attribute = attribute.substring(index);
            attribute = attribute.replace("obj", "");
            //удаляем ненужное, остается только obj
            groups.add(element.text() + "Group" + attribute);
            //
        }
        return groups;
    }
}