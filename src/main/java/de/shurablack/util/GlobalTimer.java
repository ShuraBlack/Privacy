package de.shurablack.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GlobalTimer {

    private final static ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(5);

    public static void schedule(Runnable runnable, long delay) {
        SCHEDULER.schedule(runnable, delay, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    public static void scheduleInterval(Runnable runnable, long delay, long period) {
        SCHEDULER.scheduleAtFixedRate(runnable, delay, period, TimeUnit.MILLISECONDS);
    }

}
