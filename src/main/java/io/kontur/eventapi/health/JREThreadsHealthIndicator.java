package io.kontur.eventapi.health;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;

@Component
public class JREThreadsHealthIndicator extends AbstractHealthIndicator {

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        defineThreadsCount(builder);

        builder.up();
    }

    private void defineThreadsCount(Health.Builder builder) throws IOException {
        int threadCount = ManagementFactory.getThreadMXBean().getThreadCount();

        String ulimit = "undefined";
        if (SystemUtils.IS_OS_UNIX) {
            Process exec = Runtime.getRuntime().exec("ulimit -u");
            try (InputStream stream = exec.getInputStream()) {
                ulimit = IOUtils.toString(stream);
            }
        }

        builder.withDetail("activeThreads", threadCount)
                .withDetail("maxUserProcesses", ulimit);

    }
}
