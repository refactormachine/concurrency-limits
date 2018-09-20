package com.netflix.concurrency.limits.limiter;

import com.netflix.concurrency.limits.Limiter;
import com.netflix.concurrency.limits.limit.SettableLimit;
import com.netflix.concurrency.limits.strategy.SimpleStrategy;
import org.junit.Test;

import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BlockingLimiterTest {
    @Test
    public void test() {
        SettableLimit limit = SettableLimit.startingAt(10);
        BlockingLimiter<Void> limiter = BlockingLimiter.wrap(DefaultLimiter.newBuilder().limit(limit).build(new SimpleStrategy<>()));
        
        LinkedList<Limiter.Listener> listeners = new LinkedList<>();
        for (int i = 0; i < 10; i++) {
            limiter.acquire(null).ifPresent(listeners::add);
        }
        
        limit.setLimit(1);
        
        while (!listeners.isEmpty()) {
            listeners.remove().onSuccess();
        }
        
        limiter.acquire(null);
    }

    @Test
    public void testMultipleBlockedThreads() throws InterruptedException, ExecutionException, TimeoutException {
        int numThreads = 8;
        SettableLimit limit = SettableLimit.startingAt(1);
        BlockingLimiter<Void> limiter = BlockingLimiter.wrap(DefaultLimiter.newBuilder().limit(limit).build(new SimpleStrategy<>()));
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        try {
            for (Future<?> future : IntStream.range(0, numThreads)
                    .mapToObj(x -> executorService.submit(() -> limiter.acquire(null).get().onSuccess()))
                    .collect(Collectors.toList())) {
                future.get(1, TimeUnit.SECONDS);
            }
        } finally {
            executorService.shutdown();
        }
    }
}
