package io.kontur.eventapi.health;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;

@Component
public class JREThreadsHealthIndicator extends AbstractHealthIndicator {

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        int threadCount = ManagementFactory.getThreadMXBean().getThreadCount();

        if (threadCount > 300) {
            builder.down()
                    .withDetail("activeThreadCount", threadCount)
                    .withDetail("reason", "The amount of active threads increases?");
        } else {
            builder.up();
        }

    }
}
