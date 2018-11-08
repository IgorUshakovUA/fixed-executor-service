package com.ushakov;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class FixedExecutorServiceTest {
    private static final int WAIT_TIME = 500;
    private final List<Boolean> TEST_FLAG = new ArrayList<>();
    private final Runnable TEST = () -> {
        TEST_FLAG.set(0, true);
    };
    private final Callable<Integer> CALLABLE_TEST = () -> {
        Random random = new Random();
        try {
            Thread.sleep(random.nextInt(100));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return 1;
    };

    @Test
    public void testConstructorCreatesThreads() {
        FixedExecutorService executorService = new FixedExecutorService(2);

        assertEquals(2, executorService.threadList.size());
        assertFalse(executorService.threadList.get(0).isInterrupted());
        assertFalse(executorService.threadList.get(1).isInterrupted());
    }

    @Test
    public void testShutdown() {
        FixedExecutorService executorService = new FixedExecutorService(1);

        executorService.shutdown();

        assertTrue(executorService.isShutdownInProgress);
    }

    @Test
    public void testIsShutdown() {
        ExecutorService executorService = new FixedExecutorService(1);

        assertFalse(executorService.isShutdown());
        executorService.shutdown();
        assertTrue(executorService.isShutdown());
    }

    @Test
    public void testShutdownNowReturnsCorrectList() {
        FixedExecutorService executorService = new FixedExecutorService(1);

        Runnable task = () -> {
            try {
                Thread.sleep(WAIT_TIME);
            } catch (InterruptedException e) {
                return;
            }
        };

        executorService.shutdown();
        executorService.taskList.add(task);
        List<Runnable> result = executorService.shutdownNow();
        assertEquals(0, executorService.taskList.size());
        assertEquals(1, result.size());
        assertEquals(task, result.get(0));
    }

    @Test
    public void testShutdownNowTerminatesAllThreads() throws Exception {
        FixedExecutorService executorService = new FixedExecutorService(2);

        Thread.sleep(WAIT_TIME);

        assertEquals(Thread.State.WAITING, executorService.threadList.get(0).getState());
        assertEquals(Thread.State.WAITING, executorService.threadList.get(1).getState());

        executorService.shutdownNow();

        Thread.sleep(WAIT_TIME);

        assertEquals(Thread.State.TERMINATED, executorService.threadList.get(0).getState());
        assertEquals(Thread.State.TERMINATED, executorService.threadList.get(1).getState());
    }

    @Test
    public void testTaskExecutorExecutes() throws Exception {
        FixedExecutorService executorService = new FixedExecutorService(1);
        executorService.shutdownNow();

        executorService.isShutdownInProgress = false;

        TEST_FLAG.clear();
        TEST_FLAG.add(false);

        executorService.taskList.add(TEST);
        Thread thread = new Thread(executorService.taskExecutor);
        thread.start();
        Thread.sleep(WAIT_TIME);

        assertTrue(TEST_FLAG.get(0));
    }

    @Test
    public void testIsTerminated() throws Exception {
        FixedExecutorService executorService = new FixedExecutorService(1);

        Thread.sleep(WAIT_TIME);

        assertFalse(executorService.isTerminated());

        executorService.shutdownNow();

        Thread.sleep(WAIT_TIME);

        assertTrue(executorService.isTerminated());
    }

    @Test
    public void testAwaitTermination() throws Exception {
        FixedExecutorService executorService = new FixedExecutorService(1);

        executorService.shutdownNow();

        assertTrue(executorService.awaitTermination(WAIT_TIME, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testExecute() throws Exception {
        FixedExecutorService executorService = new FixedExecutorService(1);


        TEST_FLAG.clear();
        TEST_FLAG.add(false);

        executorService.execute(TEST);

        Thread.sleep(WAIT_TIME);

        assertTrue(TEST_FLAG.get(0));
    }

    @Test
    public void testSubmitCallable() throws Exception {
        FixedExecutorService executorService = new FixedExecutorService(1);

        Callable<Integer> test = () -> {
            return 1;
        };

        Future<Integer> result = executorService.submit(test);

        assertEquals(new Integer(1), result.get());
    }

    @Test
    public void testSubmitRunnable() throws Exception {
        FixedExecutorService executorService = new FixedExecutorService(1);

        TEST_FLAG.clear();
        TEST_FLAG.add(false);

        Future<?> result = executorService.submit(TEST);

        assertNull(result.get());
    }

    @Test
    public void testSubmitRunnableWithResult() throws Exception {
        FixedExecutorService executorService = new FixedExecutorService(1);

        TEST_FLAG.clear();
        TEST_FLAG.add(false);

        String result_value = "Result value";

        Future<?> result = executorService.submit(TEST, result_value);

        assertEquals(result_value, result.get());
    }

    @Test
    public void testInvokeAll() throws Exception {
        List<Callable<Integer>> tasks = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            tasks.add(CALLABLE_TEST);
        }

        FixedExecutorService executorService = new FixedExecutorService(1);

        List<Future<Integer>> result = executorService.invokeAll(tasks);

        assertEquals(3, result.size());
        assertEquals(new Integer(1), result.get(0).get());
        assertEquals(new Integer(1), result.get(1).get());
        assertEquals(new Integer(1), result.get(2).get());
    }

    @Test
    public void testInvokeAllWithTimeout() throws Exception {
        List<Callable<Integer>> tasks = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            tasks.add(CALLABLE_TEST);
        }

        FixedExecutorService executorService = new FixedExecutorService(1);

        List<Future<Integer>> result = executorService.invokeAll(tasks, 1, TimeUnit.NANOSECONDS);

        boolean notAllAreExecuted = false;
        for (Future<Integer> future : result) {
            if (!future.isDone()) {
                notAllAreExecuted = true;
                break;
            }
        }
        assertTrue(notAllAreExecuted);

        result = executorService.invokeAll(tasks, WAIT_TIME, TimeUnit.MILLISECONDS);

        int successCount = 0;
        for (Future<Integer> future : result) {
            if (future.isDone()) {
                successCount++;
            }
        }
        notAllAreExecuted = successCount < result.size();

        assertFalse(notAllAreExecuted);
    }

    @Test
    public void testInvokeAny() throws Exception {
        List<Callable<Integer>> tasks = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            tasks.add(CALLABLE_TEST);
        }

        FixedExecutorService executorService = new FixedExecutorService(1);

        Integer result = executorService.invokeAny(tasks);

        assertEquals(new Integer(1), result);
    }

    @Test
    public void testInvokeAnyWithTimeout() throws Exception {
        List<Callable<Integer>> tasks = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            tasks.add(CALLABLE_TEST);
        }

        FixedExecutorService executorService = new FixedExecutorService(1);

        Integer result = executorService.invokeAny(tasks, 1, TimeUnit.MILLISECONDS);
        assertNull(result);
        result = executorService.invokeAny(tasks, WAIT_TIME, TimeUnit.MILLISECONDS);
        assertEquals(new Integer(1), result);
    }

}