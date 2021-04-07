package com.company.Jcommander;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

import java.io.File;

public class FileValidation implements IParameterValidator {
    @Override
    public void validate(String name, String value) throws ParameterException {
        File file = new File(value);

        if(!(file.exists() && file.isFile() && file.canRead())) {
            throw new ParameterException("File does not exist or cannot be read (");
        }
    }
}
