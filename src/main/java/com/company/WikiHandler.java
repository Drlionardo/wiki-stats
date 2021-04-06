package com.company;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

public class WikiHandler extends DefaultHandler {

    private int counter = 0;
    private WikiStats stats;
    private StringBuilder buffer = new StringBuilder();

    public WikiStats getStats() {
        return stats;
    }

    public WikiHandler(WikiStats stats) {
        this.stats = stats;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        switch (qName) {
            case "page":
                counter++;
                break;
            case "title":
                buffer = new StringBuilder();
                break;
            case "timestamp":
                buffer = new StringBuilder();
                break;
            case "text":
                sizeProcess(Integer.parseInt(attributes.getValue("bytes")));
                buffer = new StringBuilder();
                break;
            default:
                break;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        switch (qName) {
            case "page":
                if(counter%1000==0) {
                    System.out.println(Thread.currentThread().getName() + "   " + counter);
                }
                break;
            case "title":
                titleProcess(buffer.toString());
                break;
            case "timestamp":
                timestampProcess(buffer);
                break;
            case "text":
                textProcess(buffer.toString());
                break;
            default:
                break;
        }
    }

    private void textProcess(String buffer) {
        var words = buffer.split(" ");
        for(String word : words) {
            if(word.matches("[а-яА-я]{4,}")) {
                word = word.toLowerCase();
                stats.getTextWordFrequency().putIfAbsent(word, new AtomicLong(0));
                stats.getTextWordFrequency().get(word).incrementAndGet();
            }
        }
    }

    private void titleProcess(String buffer) {
        var words = buffer.split(" ");
        for(String word : words) {
            if(word.matches("[а-яА-я]{4,}")) {
                word = word.toLowerCase();
                stats.getTitleWordFrequency().putIfAbsent(word, new AtomicLong(0));
                stats.getTitleWordFrequency().get(word).incrementAndGet();
            }
        }
    }

    private void timestampProcess(StringBuilder buffer) {
        String input = buffer.toString();
        Instant i = Instant.parse(input);
        OffsetDateTime time = i.atOffset(ZoneOffset.UTC);
        Integer year = time.getYear();

        stats.getYearSpread().putIfAbsent(year, new AtomicLong(0));
        stats.getYearSpread().get(year).incrementAndGet();
    }

    private void sizeProcess(int bytes) {
        int key = 0;
        if(bytes!=0) {
            while (bytes > 0) {
                bytes /= 10;
                key++;
            }
            key -= 1;
        }

        stats.getSizeSpread().putIfAbsent(key, new AtomicLong(0));
        stats.getSizeSpread().get(key).incrementAndGet();
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        buffer.append(ch, start, length);
    }
}
