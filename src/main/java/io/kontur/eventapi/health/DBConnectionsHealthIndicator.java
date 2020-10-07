package io.kontur.eventapi.health;

import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

@Component
public class DBConnectionsHealthIndicator extends AbstractHealthIndicator {


    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName poolName = new ObjectName("com.zaxxer.hikari:type=Pool (HikariPool-1)");
        HikariPoolMXBean poolProxy = JMX.newMXBeanProxy(mBeanServer, poolName, HikariPoolMXBean.class);
        ObjectName poolConfigName = new ObjectName("com.zaxxer.hikari:type=PoolConfig (HikariPool-1)");
        HikariConfigMXBean poolConfigProxy = JMX.newMXBeanProxy(mBeanServer, poolConfigName, HikariConfigMXBean.class);

        builder.up()
                .withDetail("activeDbConnections", poolProxy.getActiveConnections())
                .withDetail("maxPoolSize", poolConfigProxy.getMaximumPoolSize());
    }
}
