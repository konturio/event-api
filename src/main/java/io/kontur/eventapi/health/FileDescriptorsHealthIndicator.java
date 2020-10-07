package io.kontur.eventapi.health;

import com.sun.management.UnixOperatingSystemMXBean;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

@Component
public class FileDescriptorsHealthIndicator extends AbstractHealthIndicator {

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        if (os instanceof UnixOperatingSystemMXBean) {
            builder.withDetail("openFileDescriptors", ((UnixOperatingSystemMXBean) os).getOpenFileDescriptorCount())
                    .withDetail("maxFileDescriptorCount", ((UnixOperatingSystemMXBean) os).getMaxFileDescriptorCount());
            builder.up();
        } else {
            builder.unknown();
        }

    }
}
