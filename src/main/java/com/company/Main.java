package com.company;

import com.beust.jcommander.JCommander;
import com.company.Jcommander.Parameters;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.xml.sax.XMLReader;

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
    public static List<File> input;
    public static File output;
    public static int threads;
    public static boolean help;

    public static void main(String[] args) {
        Parameters parameters = new Parameters();
        try {
            var jCommander = JCommander.newBuilder()
                    .addObject(parameters)
                    .build();
            jCommander.parse(args);
            input = parameters.input;
            output = parameters.output;
            threads = parameters.threads;
            help = parameters.help;

            if (help) {
                jCommander.usage();
            }
            long startTime = System.currentTimeMillis();
            startXmlParser(input, threads);
            long stopTime = System.currentTimeMillis();
            System.out.println("Time:" + (stopTime - startTime) + " ms");
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    static void startXmlParser(List<File> inputs, int numberOfThreads) throws IOException, InterruptedException {
        WikiStats stats = new WikiStats();
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        //TODO Split decompression and parsing into different tasks
        for (File file : inputs) {
            executor.execute(() -> {
                try {
                    SAXParserFactory factory = SAXParserFactory.newInstance();
                    //Disable loading dtd load to speed up work
                    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
                    SAXParser saxParser = factory.newSAXParser();
                    XMLReader xmlReader = saxParser.getXMLReader();
                    WikiHandler wikiHandler = new WikiHandler(stats);
                    xmlReader.setContentHandler(wikiHandler);
                    decompressBz2(file,file.getAbsolutePath()+".decompressed");
                    xmlReader.parse(file.getAbsolutePath()+".decompressed");
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(24L, TimeUnit.HOURS);
        printStats(stats);
    }

    private static void printStats(WikiStats stats) throws IOException {
        output.createNewFile();
        FileWriter fileWriter = new FileWriter(output);
        BufferedWriter bw = new BufferedWriter(fileWriter);

        bw.write("Топ-300 слов в заголовках статей:\n");
        top300Printer(stats.getTitleWordFrequency(), bw);
        bw.newLine();

        bw.write("Топ-300 слов в статьях:\n");
        top300Printer(stats.getTextWordFrequency(), bw);
        bw.newLine();

        var sizeEntries = new ArrayList<>(stats.getSizeSpread().entrySet());
        bw.write("Распределение статей по размеру:\n");
        statPrinter(stats.getSizeSpread(), bw, sizeEntries);
        bw.newLine();

        var yearEntries = new ArrayList<>(stats.getYearSpread().entrySet());
        bw.write("Распределение статей по времени:\n");
        statPrinter(stats.getYearSpread(), bw, yearEntries);
        bw.close();
    }

    private static void statPrinter(Map<Integer, AtomicLong> map, BufferedWriter bw, ArrayList<Map.Entry<Integer, AtomicLong>> yearEntries) throws IOException {
        var minimalEntry = yearEntries.stream().min(Map.Entry.comparingByKey());
        if(minimalEntry.isPresent()) {
        int startYear = minimalEntry.get().getKey();
        int lastYear = yearEntries.stream()
                .max(Map.Entry.comparingByKey()).get().getKey();
            for (int i = startYear; i <= lastYear; i++) {
                long amount = map.getOrDefault(i,new AtomicLong(0)).get();
                bw.write(i + " " + amount + "\n");
            }
        }
    }

    private static void top300Printer(ConcurrentMap<String, AtomicLong> map, BufferedWriter bw) throws IOException {
        List<Map.Entry<String, AtomicLong>> entryList = new ArrayList<>(map.entrySet());
        Comparator<Map.Entry<String, AtomicLong>> valueComparator = (e1, e2) -> {
            if(e2.getValue().get()==e1.getValue().get()){
                return e1.getKey().compareTo(e2.getKey());
            } else {
                return Long.compare(e2.getValue().get(), e1.getValue().get());
            }
        };
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
