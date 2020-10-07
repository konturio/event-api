package io.kontur.eventapi.health;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

@Component
public class JREMemoryHealthIndicator extends AbstractHealthIndicator {

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        builder.up();

        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        long usedMemory = memoryMXBean.getHeapMemoryUsage().getUsed();
        long maxMemory = memoryMXBean.getHeapMemoryUsage().getMax();
        long usedNonHeap = memoryMXBean.getNonHeapMemoryUsage().getUsed();
        long maxNonHeap = memoryMXBean.getNonHeapMemoryUsage().getMax();
        builder.withDetail("heap.usedMemory", usedMemory)
                .withDetail("heap.maxAvailableMemory", maxMemory)
                .withDetail("nonHeap.usedMemory", usedNonHeap)
                .withDetail("nonHeap.maxAvailableMemory", maxNonHeap == -1 ? "undefined" : maxNonHeap);
    }

}
