package io.kontur.eventapi.util;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DateTimeUtilTest {

    @Test
    public void testUniqueTimeGeneration() throws InterruptedException {
        int numberOfThreads = 100;
        AtomicBoolean success = new AtomicBoolean(true);
        ExecutorService service = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        Map<OffsetDateTime, Integer> resultMap = new HashMap<>();

        for (int i = 0; i < numberOfThreads; i++) {
            service.execute(() -> {
                OffsetDateTime offsetDateTime = DateTimeUtil.uniqueOffsetDateTime();
                if (resultMap.containsKey(offsetDateTime)) {
                    success.set(false);
                }
                resultMap.put(offsetDateTime, 0);
                latch.countDown();
            });
        }
        latch.await();
        assertTrue(success.get(), "Unique timestamps are now generated");
    }

}
