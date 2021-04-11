package com.company;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

public class WikiHandler extends DefaultHandler {
    //TODO Split parsing and counting stats into different threads
    private final WikiStats stats;
    private final StringBuilder buffer;
    private int counter = 0;
    private final StringBuilder tagPath = new StringBuilder();
    private boolean hasSizeAttribute = false;
    private String text;
    private String title;
    private String timestamp;
    private int size;

    public WikiHandler(WikiStats stats) {
        this.stats = stats;
        this.buffer = new StringBuilder();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        switch (qName) {
            case "page":
                // Reset Path at the beginning of new page
                tagPath.setLength(0);
                tagPath.append(qName).append(" ");
                hasSizeAttribute = false;
                counter++;
                break;
            case "title":
                tagPath.append(qName).append(" ");
                break;
            case "revision" :
                tagPath.append(qName).append(" ");
                break;
            case "timestamp":
                tagPath.append(qName).append(" ");
                break;
            case "text":
                tagPath.append(qName).append(" ");
                size = Integer.parseInt(attributes.getValue("bytes"));
                //TODO size tag check
                hasSizeAttribute=true;
                break;
            default:
                break;
        }
        buffer.setLength(0); //Clear buffer
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        switch (qName) {
            case "page":
                tagPath.append("/").append(qName);
                if(counter%1000==0) {
                    System.out.println(Thread.currentThread().getName() + "   " + counter);
                }
                if(isValidPage()) {
                    countStats();
                }
                break;
            case "title":
                tagPath.append("/").append(qName).append(" ");
                title = buffer.toString();
                break;
            case "revision":
                tagPath.append("/").append(qName).append(" ");
                break;
            case "timestamp":
                tagPath.append("/").append(qName).append(" ");
                timestamp = buffer.toString();
                break;
            case "text":
                tagPath.append("/").append(qName).append(" ");
                text = buffer.toString();
                break;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        buffer.append(ch, start, length);
    }

    private boolean isValidPage() {
        //Current implementation is invalid
        //TODO Update validation via xsd file
        boolean tagMatch = tagPath.toString()
                .equals("page title /title revision timestamp /timestamp text /text /revision /page");
        if(!(tagMatch && hasSizeAttribute)) {
            System.out.println("Invalid page " + counter);
            System.out.println(tagPath);
        }
        return tagMatch && hasSizeAttribute;
    }
    private void countStats() {
        textProcess(text);
        titleProcess(title);
        timestampProcess(timestamp);
        sizeProcess(size);
    }

    private void textProcess(String text) {
        text = text.replaceAll("[^а-яА-я]"," ");
        var words = text.split(" ");
        for(String word : words) {
            if(word.matches("[а-яА-я]{4,}")) {
                word = word.toLowerCase();
                stats.getTextWordFrequency().putIfAbsent(word, new AtomicLong(0));
                stats.getTextWordFrequency().get(word).incrementAndGet();
            }
        }
    }

    private void titleProcess(String title) {
        title = title.replaceAll("[^а-яА-я]"," ");
        var words = title.split(" ");
        for(String word : words) {
            if(word.matches("[а-яА-я]{4,}")) {
                word = word.toLowerCase();
                stats.getTitleWordFrequency().putIfAbsent(word, new AtomicLong(0));
                stats.getTitleWordFrequency().get(word).incrementAndGet();
            }
        }
    }

    private void timestampProcess(String timestamp) {
        Instant i = Instant.parse(timestamp);
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
}
