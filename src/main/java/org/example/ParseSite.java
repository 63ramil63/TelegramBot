package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class ParseSite {
    public static String url = "https://lk.ks.psuti.ru/?mn=2&obj=";

    /**
     *
     * @param doc doc connected to the site
     * @return string(attribute) of the next week
     */
    private static String getWK(Document doc) {
        //получаем неделю
        Elements week = doc.select("body > table:nth-child(4) > tbody > tr:nth-child(4) > td > table > tbody > tr > td:nth-child(8) > a");
        String wk = week.attr("href");
        int index = wk.indexOf("wk");
        //удаляем ненужное
        wk = wk.substring(index);
        return wk;
    }

    /**
     *
     * @param _day string of date
     * @param doc doc connected to the site
     * @return string of lessons
     */
    public static String findDay(String _day, Document doc) {
        //переменная для перебора элементов с расписанием на странице
        int num = 1;
        //перебор эл сайта до нахождения нужной даты
        while (num < 60) {
            //проверка на совпадение дат
            if (match(_day, num, doc)) {
                num++;
                return _day + getLesson(num, doc);
            }
            num++;
        }
        return "Not found";
    }

    /**
     *
     * @param _day string of date
     * @param num number of element of site
     * @param doc doc connected to site
     * @return true if text of elements on site contains string _day
     */
    public static boolean match(String _day, int num, Document doc) {
        //перебор эл сайта до нахождения нужной даты
        Elements day = doc.select("body > table:nth-child(5) > tbody > tr:nth-child(" + num + ")");
        return day.text().contains(_day);
    }

    public static String getDay(String _day, String obj) throws IOException {
        System.out.println("Parsing site for obj = " + obj);
        Document doc = Jsoup.connect(url + obj).userAgent("Chrome").timeout(10_000).get();

        //получаем расписание на эту неделю
        String lessons = findDay(_day, doc);
        if(!lessons.equals("Not found")) {
            doc.clearAttributes();
            return lessons;
        }

        //получаем страницу сайта с расписанием, которое располагается на след неделе при помощи атрибута wk
        String wk = getWK(doc);
        doc = Jsoup.connect(url + obj + "&" + wk).userAgent("Chrome").timeout(10_000).get();

        //получаем расписание на след неделю
        lessons = findDay(_day, doc);
        if(!lessons.equals("Not found")){
            doc.clearAttributes();
            return lessons;
        }
        doc.clearAttributes();
        return "Нет расписания на нужную дату \n https://lk.ks.psuti.ru/?mn=2&obj=" + obj;
    }

    /**
     *
     * @param num number of element where lessons start
     * @param doc doc connected to site
     * @return lessons
     */
    public static String getLesson(int num, Document doc) {
        //пропуск ненужного поля
        num++;
        Elements currentElement = doc.select("body > table:nth-child(5) > tbody > tr:nth-child(" + num + ")");
        //используем stringBuilder, чтобы уменьшить потребление озу
        StringBuilder lesson = new StringBuilder();

        //проверка на последний элемент расписания на день, который всегда пустой
        while (!currentElement.text().isEmpty()) {
            currentElement = doc.select("body > table:nth-child(5) > tbody > tr:nth-child(" + num + ")");
            Elements _number = doc.select("body > table:nth-child(5) > tbody > tr:nth-child(" + num + ") > td:nth-child(1)");
            Elements _time = doc.select("body > table:nth-child(5) > tbody > tr:nth-child(" + num + ") > td:nth-child(2)");
            Elements _lesson = doc.select("body > table:nth-child(5) > tbody > tr:nth-child(" + num + ") > td:nth-child(4)");
            num++;
            lesson.append("\n").append(_number.text().intern()).append(") ").append(_time.text().intern()).append(" ").append(_lesson.text().intern());
        }

        //переводим StringBuilder в String
        String lessons = lesson.toString();
        //удаление всех пробелов в конце строки, убираем лишний '(', убираем наименование места
        lessons = lessons.replaceAll("\\s+$", "").intern();
        lessons = lessons.substring(0, lessons.length() - 1).intern();
        lessons = lessons.replaceAll("Московское шоссе, 120", "").intern();
        lessons = lessons.replaceAll(" Замена Свободное время на:", "").intern();
        return lessons;
    }

    /**
     *
     * @return List of String with years of study from site
     */
    public static List<String> getYear() throws IOException {
        Document doc = Jsoup.connect("https://lk.ks.psuti.ru/?mn=2").userAgent("Chrome").timeout(10_000).get();
        List<String> years = new ArrayList<>();
        int i = 1;
        while (i != 11) {
            String year = doc.select("body > table:nth-child(5) > tbody > tr:nth-child(6) > td:nth-child(" + i + ")").text();
            if (!year.isEmpty()) {
                years.add(year + "Year" + i);
            }
            i++;
        }
        doc.clearAttributes();
        //получаем курсы и помещаем их в массив
        return years;
    }

    /**
     * writes group info in List of String
     * @param elements element from which the group is taken
     * @param groups List of String in which method write group
     * @param i index of element in elements
     */
    private static void getGroupElement(Elements elements, List<String> groups, int i) {
        Elements element = elements.select("tr:nth-child(" + i + ")");
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
    }

    /**
     *
     * @param num element number that indicates the year of study
     * @return List of String with groups in the selected year of study
     */
    public static List<String> getGroups(int num) throws IOException {
        Document doc = Jsoup.connect("https://lk.ks.psuti.ru/?mn=2").userAgent("Chrome").timeout(10_000).get();
        List<String> groups = new ArrayList<>();
        Elements elementsSize = doc.select("body > table:nth-child(5) > tbody > tr:nth-child(7) > td:nth-child(" + num + ") > table > tbody > tr:nth-child(1) > td > table > tbody > tr");
        for (int i = 1; i < elementsSize.size() + 1; i++) {
            getGroupElement(elementsSize, groups, i);
        }
        doc.clearAttributes();
        return groups;
    }
}