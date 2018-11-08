package com.ushakov;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.*;

public class FixedExecutorService implements ExecutorService {
    boolean isShutdownInProgress;
    int capacity;
    Deque<Runnable> taskList = new ConcurrentLinkedDeque<>();
    List<Thread> threadList = new ArrayList<>();

    FixedExecutorService(int capacity) {
        this.capacity = capacity;
        for (int i = 0; i < capacity; i++) {
            Thread thread = new Thread(taskExecutor);
            thread.start();
            threadList.add(thread);
        }
    }

    final Runnable taskExecutor = () -> {
        while (true) {
            Runnable task;
            while ((task = taskList.poll()) == null) {
                if (Thread.interrupted() || isShutdownInProgress) {
                    return;
                }
                synchronized (taskList) {
                    try {
                        taskList.wait();
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
            task.run();
        }
    };

    public void shutdown() {
        isShutdownInProgress = true;
    }

    public List<Runnable> shutdownNow() {
        List<Runnable> result = new ArrayList<>();

        isShutdownInProgress = true;

        for (Runnable runnable : taskList) {
            result.add(runnable);
        }

        for (Thread thread : threadList) {
            thread.interrupt();
        }

        synchronized (taskList) {
            taskList.clear();
            taskList.notifyAll();
        }

        return result;
    }

    public boolean isShutdown() {
        return isShutdownInProgress;
    }

    public boolean isTerminated() {
        boolean result = false;

        if (isShutdownInProgress) {
            int terminatedCount = 0;
            for (Thread thread : threadList) {
                if (thread.getState().equals(Thread.State.TERMINATED)) {
                    terminatedCount++;
                }
            }
            result = (terminatedCount == threadList.size());
        }

        return result;
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        if (!isTerminated()) {
            synchronized (taskList) {
                taskList.wait(unit.toMillis(timeout));
            }
        }
        return isTerminated();
    }

    public <T> Future<T> submit(Callable<T> task) {
        Object monitor = new Object();
        CallableToRunnableAdapter<T> adapter = new CallableToRunnableAdapter(task, monitor);
        synchronized (taskList) {
            taskList.add(adapter);
            taskList.notify();
        }
        synchronized (monitor) {
            while (!adapter.isDone()) {
                try {
                    monitor.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return adapter;
    }

    public <T> Future<T> submit(Runnable task, T result) {
        DummyFuture<T> returnig_value = new DummyFuture<>(result);

        submit(task);

        return returnig_value;
    }

    public Future<?> submit(Runnable task) {
        Object monitor = new Object();
        MonitoredRunnableDecorator decorator = new MonitoredRunnableDecorator(task, monitor);
        synchronized (taskList) {
            taskList.add(decorator);
            taskList.notify();
        }
        synchronized (monitor) {
            while (!decorator.isDone()) {
                try {
                    monitor.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        Future<?> result = new DummyFuture(null);
        return result;
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        Object monitor = new Object();
        List<CallableToRunnableAdapter<T>> adapterList = new ArrayList<>();

        for (Callable<T> task : tasks) {
            CallableToRunnableAdapter<T> adapter = new CallableToRunnableAdapter<>(task, monitor);
            adapterList.add(adapter);
        }

        synchronized (taskList) {
            taskList.addAll(adapterList);
            taskList.notify();
        }

        while (true) {
            synchronized (monitor) {
                boolean isDone = true;
                for (CallableToRunnableAdapter<T> adapter : adapterList) {
                    if (!adapter.isDone()) {
                        isDone = false;
                        break;
                    }
                }
                if (!isDone) {
                    monitor.wait();
                } else {
                    break;
                }
            }
        }

        List<Future<T>> futureList = new ArrayList<>();

        futureList.addAll(adapterList);

        return futureList;
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        long timeToWait = unit.toMillis(timeout);
        LocalDateTime startTime = LocalDateTime.now();

        Object monitor = new Object();
        List<CallableToRunnableAdapter<T>> adapterList = new ArrayList<>();

        for (Callable<T> task : tasks) {
            CallableToRunnableAdapter<T> adapter = new CallableToRunnableAdapter<>(task, monitor);
            adapterList.add(adapter);
        }

        synchronized (taskList) {
            taskList.addAll(adapterList);
            taskList.notify();
        }

        while (true) {
            synchronized (monitor) {
                boolean isDone = true;
                for (CallableToRunnableAdapter<T> adapter : adapterList) {
                    if (!adapter.isDone()) {
                        isDone = false;
                        break;
                    }
                }
                if (!isDone) {
                    if (Duration.between(startTime, LocalDateTime.now()).toMillis() > timeToWait) {
                        break;
                    }
                    long newTimeout = timeToWait - Duration.between(startTime, LocalDateTime.now()).toMillis();
                    monitor.wait(newTimeout, 0);
                } else {
                    break;
                }
            }
        }

        List<Future<T>> futureList = new ArrayList<>();

        futureList.addAll(adapterList);

        return futureList;
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        Object monitor = new Object();
        List<CallableToRunnableAdapter<T>> adapterList = new ArrayList<>();

        for (Callable<T> task : tasks) {
            CallableToRunnableAdapter<T> adapter = new CallableToRunnableAdapter<>(task, monitor);
            adapterList.add(adapter);
        }

        synchronized (taskList) {
            taskList.addAll(adapterList);
            taskList.notify();
        }

        while (true) {
            synchronized (monitor) {
                for (CallableToRunnableAdapter<T> adapter : adapterList) {
                    if (adapter.isDone()) {
                        return adapter.get();
                    }
                }
                monitor.wait();
            }
        }
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        long timeToWait = unit.toMillis(timeout);
        LocalDateTime startTime = LocalDateTime.now();
        Object monitor = new Object();
        List<CallableToRunnableAdapter<T>> adapterList = new ArrayList<>();

        for (Callable<T> task : tasks) {
            CallableToRunnableAdapter<T> adapter = new CallableToRunnableAdapter<>(task, monitor);
            adapterList.add(adapter);
        }

        synchronized (taskList) {
            taskList.addAll(adapterList);
            taskList.notify();
        }

        while (true) {
            synchronized (monitor) {
                for (CallableToRunnableAdapter<T> adapter : adapterList) {
                    if (adapter.isDone()) {
                        return adapter.get();
                    }
                }
                long newTimeout = timeToWait - Duration.between(startTime, LocalDateTime.now()).toMillis();
                if (newTimeout <= 0) {
                    return null;
                }
                monitor.wait(newTimeout, 0);
            }
        }
    }

    public void execute(Runnable command) {
        synchronized (taskList) {
            taskList.add(command);
            taskList.notify();
        }
    }
}
