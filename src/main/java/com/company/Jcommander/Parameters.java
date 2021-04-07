package com.company.Jcommander;

import com.beust.jcommander.Parameter;

import java.io.File;
import java.util.List;

public class Parameters {
    @Parameter(names = "--inputs", converter = FileConverter.class, validateWith = FileValidation.class,
            description = "Path(s) to bzip2 archived XML file(s) with WikiMedia dump. Comma separated.")
    public List<File> inputs;
    @Parameter(names = "--output", converter = FileConverter.class, validateWith = FileValidation.class,
            description = "Report output file")
    public File output = new File("statistics.md");
    @Parameter(names = "--threads", validateWith = ThreadsValidation.class,
            description = "Number of threads")
    public int threads = 4;
    @Parameter(names = "--help", help = true)
    public boolean help;
}
