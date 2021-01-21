package io.kontur.eventapi.job;

import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractJob implements Runnable {
    private final static Map<String, Lock> locks = new ConcurrentHashMap<>();

    @Override
    public void run() {
        String jobName = this.getClass().getName();
        Logger logger = LoggerFactory.getLogger(this.getClass());

        locks.putIfAbsent(jobName, new ReentrantLock());
        Lock lock = locks.get(jobName);

        if (!lock.tryLock()) {
            printThreadDump();
            throw new IllegalStateException("Parallel execution of same job is not supported, job" + jobName);
        }

        logger.info("job has started");
        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            execute();
        } catch (Exception e) {
            stopwatch.stop();
            logger.error("job has failed after {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS), e);

            throw new RuntimeException("failed job " + jobName, e);
        } finally {
            lock.unlock();
        }

        stopwatch.stop();
        long time = stopwatch.elapsed(TimeUnit.SECONDS);
        logger.info("job has finished in {} seconds", time);
        if (time > 60) {
            logger.warn("[slow_job] {} seconds", time);
        }
    }

    private void printThreadDump() {
        ThreadInfo[] threads = ManagementFactory.getThreadMXBean().dumpAllThreads(true, true);
        for (ThreadInfo info : threads) {
            System.out.print(info);
        }
    }

    public abstract void execute() throws Exception;
}
