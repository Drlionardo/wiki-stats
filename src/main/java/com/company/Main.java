package com.company;

import com.beust.jcommander.JCommander;
import com.company.Jcommander.Parameters;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


public class Main {
    public static void main(String[] args) {
        Parameters parameters = new Parameters();
        try {
            JCommander.newBuilder()
                    .addObject(parameters)
                    .build()
                    .parse(args);
            if (parameters.help) {
                //TODO print help info
            }

            startXmlParser(parameters.inputs, parameters.threads);

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void measureTime(){
        //TODO measure time
        // startXmlParser();
    }

    static void startXmlParser(List<File> inputs, int numberOfThreads) throws IOException, ParserConfigurationException, SAXException, InterruptedException {
        WikiStats stats = new WikiStats();
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        //TODO Split decompression and parsing into different tasks
        for (File file : inputs) {
            executor.submit(() -> {
                try {
                    SAXParserFactory factory = SAXParserFactory.newInstance();
                    SAXParser saxParser = factory.newSAXParser();
                    WikiHandler wikiHandler = new WikiHandler(stats);
                    decompressBz2(file,file.getAbsolutePath()+"decompressed");
                    saxParser.parse(file.getAbsolutePath()+"decompressed", wikiHandler);
                } catch (Exception ignored) {
                }
            });
        }
        executor.shutdown();
        while (!executor.awaitTermination(24L, TimeUnit.HOURS)) {
            System.out.println("Not yet. Still waiting for termination");
        }

        printStats(stats);
    }

    private static void printStats(WikiStats stats) {
        System.out.println("Топ-300 слов в заголовках статей:");
        mapSortTop300(stats.getTitleWordFrequency());
        System.out.println();

        System.out.println("Топ-300 слов в статьях:");
        mapSortTop300(stats.getTextWordFrequency());
        System.out.println();

        //TODO print first keys even if value=0
        System.out.println("Распределение статей по размеру:");
        mapSortTop(stats.getSizeSpread());
        System.out.println();

        System.out.println("Распределение статей по времени:");
        mapSortTop(stats.getYearSpread());
    }

    private static void mapSortTop(ConcurrentMap<Integer, AtomicLong> map) {
        List<Map.Entry<Integer, AtomicLong>> entryList = new ArrayList<>(map.entrySet());
        Comparator<Map.Entry<Integer, AtomicLong>> valueComparator =
                Comparator.comparingLong(Map.Entry::getKey);
        entryList.sort(valueComparator);
        for (var integerAtomicLongEntry : entryList) {
            System.out.println(integerAtomicLongEntry.getKey() + " " + integerAtomicLongEntry.getValue());
        }

    }
    private static void mapSortTop300(ConcurrentMap<String, AtomicLong> map) {
        List<Map.Entry<String, AtomicLong>> entryList = new ArrayList<>(map.entrySet());
        Comparator<Map.Entry<String, AtomicLong>> valueComparator =
                (e1, e2) -> Long.compare(e2.getValue().get(), e1.getValue().get());
        entryList.sort(valueComparator);
        for (int i = 0; i < 300; i++) {
            AtomicLong freq = entryList.get(i).getValue();
            String words = entryList.get(i).getKey();
            System.out.println(freq + " " + words);
        }
    }

    private static void decompressBz2(File inputFile, String outputFile) throws IOException {
        var input = new BZip2CompressorInputStream(new BufferedInputStream(new FileInputStream(inputFile)));
        var output = new FileOutputStream(outputFile);
        try (input; output) {
            IOUtils.copy(input, output);
        }
        System.out.println("file converted");
    }
}
