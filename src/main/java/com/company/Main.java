package com.company;

import com.beust.jcommander.JCommander;
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


public class Main {
    public static void main(String[] args) {
        Parameters parameters = new Parameters();
        try {
            JCommander.newBuilder()
                    .addObject(parameters)
                    .build()
                    .parse(args);

            if(parameters.help) {
                //TODO print help info
            }
            startXmlParser(parameters.inputs);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void measureTime(){
        //TODO measure time
        // startXmlParser();
    }

    static void startXmlParser(List<File> inputs) throws IOException, ParserConfigurationException, SAXException {
        for(File f : inputs) {
            decompressBz2(f,f.getAbsolutePath()+"decompressed");
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            WikiHandler wikiHandler = new WikiHandler();
            saxParser.parse(f.getAbsolutePath()+"decompressed", wikiHandler);
            System.out.println("File parsed");

            Comparator<Map.Entry<String, Long>> valueComparator = (e1, e2) -> e2.getValue().compareTo(e1.getValue());
            List<Map.Entry<String, Long>> entryList = new ArrayList<>(wikiHandler.getStats().getTextWordFrequency().entrySet());
            entryList.sort(valueComparator);
            for (int i = 0; i < 300; i++) {
                long freq = entryList.get(i).getValue();
                String words = entryList.get(i).getKey();
                System.out.println(i+")"+freq +" " + words);
            }
        }
    }

    public static void decompressBz2(File inputFile, String outputFile) throws IOException {
        var input = new BZip2CompressorInputStream(new BufferedInputStream(new FileInputStream(inputFile)));
        var output = new FileOutputStream(outputFile);
        try (input; output) {
            IOUtils.copy(input, output);
        }
    }
}
