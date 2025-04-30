package de.shurablack.util;

public class Time {

    public static long get() {
        return System.currentTimeMillis();
    }

    public static long getPlusSeconds(long seconds) {
        return System.currentTimeMillis() + seconds * 1_000L;
    }

}