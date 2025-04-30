package de.shurablack.websocket.task;

public class InterruptibleTask implements Runnable {

    private volatile Thread currentThread;

    private final Runnable task;

    public InterruptibleTask(Runnable task) {
        this.task = task;
    }

    @Override
    public void run() {
        currentThread = Thread.currentThread();
        try {
            task.run();
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        } finally {
            currentThread = null;
        }

    }

    public void interrupt() {
        if (currentThread != null) {
            currentThread.interrupt();
        }
    }

}
