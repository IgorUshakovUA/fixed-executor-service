package com.ushakov;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class MonitoredRunnableDecoratorTest {
    @Test
    public void testRun() throws Exception {
        Object monitor = new Object();
        List<Boolean> testFlag = new ArrayList<>();
        testFlag.add(false);
        Runnable runnable = () -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            testFlag.set(0, true);
        };
        MonitoredRunnableDecorator decorator = new MonitoredRunnableDecorator(runnable, monitor);
        Thread thread = new Thread(decorator);
        thread.start();
        synchronized (monitor) {
            while (!decorator.isDone()) {
                monitor.wait();
            }
        }
        assertTrue(testFlag.get(0));
    }

}