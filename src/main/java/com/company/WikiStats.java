package com.company;

import java.util.HashMap;

public class WikiStats {
    private HashMap<String, Long> titleWordFrequency;
    private HashMap<String, Long> textWordFrequency;
    private HashMap<Integer, Long> yearSpread;
    private HashMap<Integer, Long> sizeSpread;

    public WikiStats() {
        this.titleWordFrequency = new HashMap<>();
        this.textWordFrequency = new HashMap<>();
        this.yearSpread = new HashMap<>();
        this.sizeSpread = new HashMap<>();
    }

    public HashMap<String, Long> getTitleWordFrequency() {
        return titleWordFrequency;
    }

    public void setTitleWordFrequency(HashMap<String, Long> titleWordFrequency) {
        this.titleWordFrequency = titleWordFrequency;
    }

    public HashMap<String, Long> getTextWordFrequency() {
        return textWordFrequency;
    }

    public void setTextWordFrequency(HashMap<String, Long> textWordFrequency) {
        this.textWordFrequency = textWordFrequency;
    }

    public HashMap<Integer, Long> getYearSpread() {
        return yearSpread;
    }

    public void setYearSpread(HashMap<Integer, Long> yearSpread) {
        this.yearSpread = yearSpread;
    }

    public HashMap<Integer, Long> getSizeSpread() {
        return sizeSpread;
    }

    public void setSizeSpread(HashMap<Integer, Long> sizeSpread) {
        this.sizeSpread = sizeSpread;
    }
}
