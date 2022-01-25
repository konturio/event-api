package io.kontur.eventapi.job;

import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractJob implements Runnable {
    private final static Map<String, Lock> locks = new ConcurrentHashMap<>();
    private final MeterRegistry meterRegistry;

    protected AbstractJob(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

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

        logger.debug("job has started");
        Timer.Sample regularTimer = Timer.start(meterRegistry);
        LongTaskTimer.Sample longTaskTimer = LongTaskTimer.builder("job." + getName() + ".current")
                .register(meterRegistry).start();

        try {
            execute();
        } catch (Exception e) {
            long duration = stopTimer(regularTimer);
            longTaskTimer.stop();
            logger.error("job has failed after {} seconds", duration, e);

            throw new RuntimeException("failed job " + jobName, e);
        } finally {
            lock.unlock();
        }

        long duration = stopTimer(regularTimer);
        longTaskTimer.stop();
        logger.debug("job has finished in {} seconds", duration);
        if (duration > 60) {
            logger.warn("[slow_job] {} seconds", duration);
        }
    }

    private long stopTimer(Timer.Sample timer) {
        long durationInNS = timer.stop(Timer.builder("job." + getName()).register(meterRegistry));
        return durationInNS / 1_000_000_000;
    }

    private void printThreadDump() {
        ThreadInfo[] threads = ManagementFactory.getThreadMXBean().dumpAllThreads(true, true);
        for (ThreadInfo info : threads) {
            System.out.print(info);
        }
    }

    public abstract void execute() throws Exception;

    public abstract String getName();
}
