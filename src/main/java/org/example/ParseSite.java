package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;

public class ParseSite {
    private Document doc;
    public String getDay(String _day) throws IOException {
        doc = Jsoup.connect("https://lk.ks.psuti.ru/?mn=2&obj=141").userAgent("Chrome").get();
        int num = 1;
        while(num < 60){
            Elements day = doc.select("body > table:nth-child(5) > tbody > tr:nth-child(" + num + ")");
            num ++;
            if(day.text().contains(_day)){
                return _day + getLesson(num);
            }
        }
        return "ошибка \n https://lk.ks.psuti.ru/?mn=2&obj=141";
    }

    public String getLesson(int num) {
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
