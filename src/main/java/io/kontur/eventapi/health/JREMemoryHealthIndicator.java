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
        long usedHeap = memoryMXBean.getHeapMemoryUsage().getUsed();
        long maxHeap = memoryMXBean.getHeapMemoryUsage().getMax();
        long usedNonHeap = memoryMXBean.getNonHeapMemoryUsage().getUsed();
        long maxNonHeap = memoryMXBean.getNonHeapMemoryUsage().getMax();

        if (usedHeap * 100 / maxHeap > 80 || usedNonHeap * 100 / maxNonHeap > 80) {
            builder.down()
                    .withDetail("reason", "At lest one of the memory category is filled for more than 80 percent")
                    .withDetail("heap.usedMemory", usedHeap)
                    .withDetail("heap.maxAvailableMemory", maxHeap)
                    .withDetail("nonHeap.usedMemory", usedNonHeap)
                    .withDetail("nonHeap.maxAvailableMemory", maxNonHeap == -1 ? "undefined" : maxNonHeap);
        }

    }

}
