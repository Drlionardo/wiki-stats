package com.company.Jcommander;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class ThreadsValidation implements IParameterValidator {
    @Override
    public void validate(String name, String value) throws ParameterException {
        int threads = Integer.parseInt(value);
        if(!(threads >= 1 && threads <= 32 )) {
            throw new ParameterException("Parameter " + name + " should be in range from 1 to 32 (found " + value +")");
        }
    }
}
