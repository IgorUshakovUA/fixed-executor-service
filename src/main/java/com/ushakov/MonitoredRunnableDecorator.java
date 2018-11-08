package com.ushakov;

public class MonitoredRunnableDecorator implements Runnable {
    private Object monitor;
    private Runnable task;
    private boolean isDone;

    public MonitoredRunnableDecorator(Runnable task, Object monitor) {
      this.monitor = monitor;
      this.task = task;
    }

    @Override
    public void run() {
        task.run();
        isDone = true;
        synchronized (monitor) {
            monitor.notify();
        }
    }

    public boolean isDone() {
        return isDone;
    }
}
