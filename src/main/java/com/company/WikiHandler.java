package com.company;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class WikiHandler extends DefaultHandler {
    private final static int THREAD_SIZE = Runtime.getRuntime().availableProcessors();
    private final WikiStats stats;
    private final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_SIZE);
    private final Semaphore lock = new Semaphore(THREAD_SIZE * 2);
    private final StringBuilder buffer;
    private final StringBuilder tagPath = new StringBuilder();
    private boolean hasSizeAttribute = false;

    private String text;
    private String title;
    private String timestamp;
    private int size;

    class Task implements Runnable {
        private final String taskText;
        private final String taskTitle;
        private final String taskTimestamp;
        private final int taskSize;

        public Task(String text, String title, String timestamp, int size) {
            this.taskText = text;
            this.taskTitle = title;
            this.taskTimestamp = timestamp;
            this.taskSize = size;
        }

        @Override
        public void run() {
            countStats();
        }
        private void countStats() {
            textProcess(taskText);
            titleProcess(taskTitle);
            timestampProcess(taskTimestamp);
            sizeProcess(taskSize);
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

    public WikiHandler(WikiStats stats) {
        this.stats = stats;
        this.buffer = new StringBuilder();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        switch (qName) {
            case "page":
                // Reset Path at the beginning of new page
                tagPath.setLength(0);
                tagPath.append(qName).append(" ");
                hasSizeAttribute = false;
                break;
            case "title":
            case "revision" :
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
                if(isValidPage()) {
                    try {
                        lock.acquire();
                        Task currentTask = new Task(text, title, timestamp, size);
                        executorService.submit(() -> {
                            try {
                                currentTask.run();
                            } finally {
                                lock.release();
                            }
                        });
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
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
        //TODO Rewrite validation. Current implementation may not be valid for different tags inside page
        boolean tagMatch = tagPath.toString()
                .equals("page title /title revision timestamp /timestamp text /text /revision /page");
        return tagMatch && hasSizeAttribute;
    }

    @Override
    public void endDocument(){
        executorService.shutdown();
        try {
            executorService.awaitTermination(24L, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
