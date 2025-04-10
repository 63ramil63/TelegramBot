package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class ParseSite {
    private static Document doc;
    public static String url = "https://lk.ks.psuti.ru/?mn=2&obj=";

    public static String getDay(String _day, String obj) throws IOException {
        System.out.println("Parsing site for obj = " + obj);
        doc = Jsoup.connect(url + obj).userAgent("Chrome").get();
        int num = 1;
        while (num < 60) {
            //перебор эл сайта до нахождения нужной даты
            if (match(_day, num, doc)) {
                //проверка на совпадение дат
                num++;
                String lessons = getLesson(num);
                return _day + lessons;
            }
            num++;
        }
        Elements week = doc.select("body > table:nth-child(4) > tbody > tr:nth-child(4) > td > table > tbody > tr > td:nth-child(8) > a");
        //получаем неделю
        String wk = week.attr("href");
        int index = wk.indexOf("wk");
        //удаляем ненужное
        wk = wk.substring(index);
        num = 1;
        //получаем расписание, которое располагается на след неделе при помощи атрибута wk
        Document newDoc = Jsoup.connect(url + obj + "&" + wk).userAgent("Chrome").get();
        while (num < 60) {
            //перебор эл сайта до нахождения нужной даты
            if (match(_day, num, newDoc)) {
                num++;
                String lessons = getLesson(num);
                return _day + lessons;
            }
            num++;
        }
        return "ошибка \n https://lk.ks.psuti.ru/?mn=2&obj=" + obj;
    }

    public static boolean match(String _day, int num, Document doc) {
        //перебор эл сайта до нахождения нужной даты
        Elements day = doc.select("body > table:nth-child(5) > tbody > tr:nth-child(" + num + ")");
        return day.text().contains(_day);
    }


    public static String getLesson(int num) {
        num++;     //чтобы не выбирало ненужные поля
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
        //удаление всех пробелов в конце строки, убираем лишний '(', убираем наименование места
        lessons = lessons.replaceAll("\\s+$", "");
        lessons = lessons.substring(0, lessons.length() - 1);
        lessons = lessons.replaceAll("Московское шоссе, 120", "");
        lessons = lessons.replaceAll(" Замена Свободное время на:", "");
        return lessons;
    }


    public static List<String> getYear() throws IOException {
        Document doc = Jsoup.connect("https://lk.ks.psuti.ru/?mn=2").userAgent("Chrome").get();
        List<String> years = new ArrayList<>();
        int i = 1;
        while (i != 11) {
            String year = doc.select("body > table:nth-child(5) > tbody > tr:nth-child(6) > td:nth-child(" + i + ")").text();
            if (!year.isEmpty()) {
                years.add(year + "Year" + i);
            }
            i++;
        }
        //получаем курсы и помещаем их в массив
        return years;
    }

    public static List<String> getGroups(int num) throws IOException {
        Document doc = Jsoup.connect("https://lk.ks.psuti.ru/?mn=2").userAgent("Chrome").get();
        List<String> groups = new ArrayList<>();
        Elements elementsSize = doc.select("body > table:nth-child(5) > tbody > tr:nth-child(7) > td:nth-child(" + num + ") > table > tbody > tr:nth-child(1) > td > table > tbody > tr");
        for (int i = 1; i < elementsSize.size() + 1; i++) {
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