package com.company;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class WikiStats {
    private ConcurrentMap<String, AtomicLong> titleWordFrequency;
    private ConcurrentMap<String, AtomicLong> textWordFrequency;
    private ConcurrentMap<Integer, AtomicLong> yearSpread;
    private ConcurrentMap<Integer, AtomicLong>  sizeSpread;

    public WikiStats() {
        this.titleWordFrequency = new ConcurrentHashMap<>();
        this.textWordFrequency = new ConcurrentHashMap<>();
        this.yearSpread = new ConcurrentHashMap<>();
        this.sizeSpread = new ConcurrentHashMap<>();
    }

    public ConcurrentMap<String, AtomicLong> getTitleWordFrequency() {
        return titleWordFrequency;
    }

    public ConcurrentMap<String, AtomicLong> getTextWordFrequency() {
        return textWordFrequency;
    }

    public ConcurrentMap<Integer, AtomicLong> getYearSpread() {
        return yearSpread;
    }

    public ConcurrentMap<Integer, AtomicLong> getSizeSpread() {
        return sizeSpread;
    }
}
