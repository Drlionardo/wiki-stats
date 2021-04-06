package com.company.Jcommander;

import com.beust.jcommander.Parameter;
import com.company.Jcommander.FileConverter;

import java.io.File;
import java.util.List;

public class Parameters {
    @Parameter(names = "--inputs", converter = FileConverter.class,
            description = "Path(s) to bzip2 archived XML file(s) with WikiMedia dump. Comma separated.")
    public List<File> inputs;
    @Parameter(names = "--output", converter = FileConverter.class,
            description = "Report output file")
    public File output = new File("statistics.txt");
    @Parameter(names = "--threads", description = "Number of threads")
    public int threads = 4;
    @Parameter(names = "--help", help = true)
    public boolean help;
}
