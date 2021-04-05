package com.company;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class WikiHandler extends DefaultHandler {

    private int counter = 0;
    private WikiStats stats;
    private StringBuilder buffer = new StringBuilder();

    public WikiStats getStats() {
        return stats;
    }

    @Override
    public void startDocument() throws SAXException {
        stats = new WikiStats();
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

    private void sizeProcess(int bytes) {
        int key = 0;
        if(bytes!=0) {
            while (bytes > 0) {
                bytes /= 10;
                key++;
            }
            key -= 1;
        }
        Long counter = stats.getSizeSpread().getOrDefault(key, 0L);
        stats.getSizeSpread().put(key, counter + 1);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        switch (qName) {
            case "page":
                System.out.println(counter);
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
                Long count = stats.getTextWordFrequency().getOrDefault(word, 0L);
                stats.getTextWordFrequency().put(word, count + 1);
            }
        }
    }

    private void titleProcess(String buffer) {
        var words = buffer.split(" ");
        for(String word : words) {
            if(word.matches("[а-яА-я]{4,}")) {
                word = word.toLowerCase();
                Long count = stats.getTitleWordFrequency().getOrDefault(word, 0L);
                stats.getTitleWordFrequency().put(word, count + 1);
            }
        }
    }

    private void timestampProcess(StringBuilder buffer) {
        String input = buffer.toString();
        Instant i = Instant.parse(input);
        OffsetDateTime time = i.atOffset(ZoneOffset.UTC);
        Integer year = time.getYear();

        Long count = stats.getYearSpread().getOrDefault(year, 0L);
        stats.getYearSpread().put(year, count + 1);
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        buffer.append(ch, start, length);
    }
}
