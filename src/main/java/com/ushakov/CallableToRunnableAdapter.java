package com.ushakov;

import java.util.concurrent.*;

public class CallableToRunnableAdapter<T> implements Runnable, Future<T> {
    private Callable<T> callable;
    private T result;
    private boolean isFinished;
    private Object monitor;

    public CallableToRunnableAdapter(Callable<T> callable, Object monitor) {
        this.callable = callable;
        this.monitor = monitor;
    }

    @Override
    public void run() {
        try {
            result = callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        isFinished = true;
        synchronized (monitor) {
            monitor.notifyAll();
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new RuntimeException("This future is not supported!");
    }

    @Override
    public boolean isCancelled() {
        throw new RuntimeException("This future is not supported!");
    }

    @Override
    public boolean isDone() {
        return isFinished;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        synchronized (monitor) {
            while (!isFinished) {
                monitor.wait();
            }
            return result;
        }
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        throw new RuntimeException("This future is not supported!");
    }
}
