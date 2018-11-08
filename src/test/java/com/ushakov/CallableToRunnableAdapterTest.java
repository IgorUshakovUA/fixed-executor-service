package com.ushakov;

import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class CallableToRunnableAdapterTest {
    private static final Callable<Integer> TEST = () -> {
        return 1;
    };

    @Test
    public void testRunSuccess()  throws Exception {
        CallableToRunnableAdapter<Integer> callableToRunnableAdapter = new CallableToRunnableAdapter<>(TEST, this);
        assertFalse(callableToRunnableAdapter.isDone());
        callableToRunnableAdapter.run();
        assertTrue(callableToRunnableAdapter.isDone());
        assertEquals(new Integer(1), callableToRunnableAdapter.get());
    }

    @Test(expected = RuntimeException.class)
    public void testRunThrowsException() {
        Callable<Integer> test = () -> {
            throw new RuntimeException("Test exception");
        };

        CallableToRunnableAdapter<Integer> callableToRunnableAdapter = new CallableToRunnableAdapter<>(test, this);
        callableToRunnableAdapter.run();
    }

    @Test(expected = RuntimeException.class)
    public void testCancell() {
        CallableToRunnableAdapter<Integer> callableToRunnableAdapter = new CallableToRunnableAdapter<>(TEST, this);
        callableToRunnableAdapter.cancel(true);
    }

    @Test(expected = RuntimeException.class)
    public void testIsCancelled() {
        CallableToRunnableAdapter<Integer> callableToRunnableAdapter = new CallableToRunnableAdapter<>(TEST, this);
        boolean b = callableToRunnableAdapter.isCancelled();
    }

    @Test(expected = RuntimeException.class)
    public void testGetWitTimeout() throws Exception {
        CallableToRunnableAdapter<Integer> callableToRunnableAdapter = new CallableToRunnableAdapter<>(TEST, this);
        Integer i = callableToRunnableAdapter.get(100, TimeUnit.MILLISECONDS);
    }

}