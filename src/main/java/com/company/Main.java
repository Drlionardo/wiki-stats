package com.company;

import com.beust.jcommander.JCommander;
import com.company.Jcommander.Parameters;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


public class Main {
    public static List<File> inputs;
    public static File output;
    public static int threads;
    public static boolean help;

    public static void main(String[] args) {
        Parameters parameters = new Parameters();
        try {
            JCommander.newBuilder()
                    .addObject(parameters)
                    .build()
                    .parse(args);
            inputs = parameters.inputs;
            output = parameters.output;
            threads = parameters.threads;
            help = parameters.help;

            if (help) {
                //TODO print help info
            }
            long startTime = System.currentTimeMillis();
            startXmlParser(inputs, threads);
            long stopTime = System.currentTimeMillis();
            System.out.println("Time:" + (stopTime - startTime) + " ms");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void startXmlParser(List<File> inputs, int numberOfThreads) throws IOException, InterruptedException {
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

    private static void printStats(WikiStats stats) throws IOException {
        output.createNewFile();
        FileWriter fileWriter = new FileWriter(output);
        BufferedWriter bw = new BufferedWriter(fileWriter);

        bw.write("Топ-300 слов в заголовках статей:\n");
        mapSortTop300(stats.getTitleWordFrequency(), bw);
        bw.newLine();

        bw.write("Топ-300 слов в статьях:\n");
        mapSortTop300(stats.getTextWordFrequency(), bw);
        bw.newLine();

        var sizeEntries = new ArrayList<>(stats.getSizeSpread().entrySet());
        mapSortTop(sizeEntries);
        bw.write("Распределение статей по размеру:\n");
        for (int i = 0; i < sizeEntries.get(0).getKey(); i++) {
            bw.write(i + " " + 0);
        }
        for (var integerAtomicLongEntry : sizeEntries) {
            bw.write(integerAtomicLongEntry.getKey() + " " + integerAtomicLongEntry.getValue() + "\n");
        }
        bw.newLine();

        var yearEntries = new ArrayList<>(stats.getYearSpread().entrySet());
        mapSortTop(yearEntries);
        bw.write("Распределение статей по времени:\n");
        for (var integerAtomicLongEntry : yearEntries) {
            bw.write(integerAtomicLongEntry.getKey() + " " + integerAtomicLongEntry.getValue() + "\n");
        }
        bw.close();
    }

    private static void mapSortTop(ArrayList<Map.Entry<Integer, AtomicLong>> entries) {
        Comparator<Map.Entry<Integer, AtomicLong>> valueComparator =
                Comparator.comparingLong(Map.Entry::getKey);
        entries.sort(valueComparator);
    }
    private static void mapSortTop300(ConcurrentMap<String, AtomicLong> map, BufferedWriter bw) throws IOException {
        List<Map.Entry<String, AtomicLong>> entryList = new ArrayList<>(map.entrySet());
        Comparator<Map.Entry<String, AtomicLong>> valueComparator =
                (e1, e2) -> Long.compare(e2.getValue().get(), e1.getValue().get());
        entryList.sort(valueComparator);
        for (int i = 0; i < (Math.min(entryList.size(), 300)); i++) {
            AtomicLong freq = entryList.get(i).getValue();
            String words = entryList.get(i).getKey();
            bw.write(freq + " " + words + "\n");
        }
    }

    private static void decompressBz2(File inputFile, String outputFile) throws IOException {
        var input = new BZip2CompressorInputStream(new BufferedInputStream(new FileInputStream(inputFile)));
        var output = new FileOutputStream(outputFile);
        try (input; output) {
            IOUtils.copy(input, output);
        }
    }
}
