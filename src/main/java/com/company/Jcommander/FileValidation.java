package com.company.Jcommander;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

import java.io.File;

public class FileValidation implements IParameterValidator {
    @Override
    public void validate(String name, String value) throws ParameterException {
        for(String filename : value.split(",")) {
            File file = new File(filename);
            if (!(file.exists() && file.isFile() && file.canRead())) {
                throw new RuntimeException("File does not exist or cannot be read (");
            }
        }
    }
}
