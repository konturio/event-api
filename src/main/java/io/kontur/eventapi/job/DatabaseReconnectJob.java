package io.kontur.eventapi.job;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPoolMXBean;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Periodically checks database connectivity and evicts connections
 * from Hikari pool when the database becomes unavailable.
 */
@Component
public class DatabaseReconnectJob extends AbstractJob {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseReconnectJob.class);
    private final HikariDataSource dataSource;

    public DatabaseReconnectJob(MeterRegistry meterRegistry, HikariDataSource dataSource) {
        super(meterRegistry);
        this.dataSource = dataSource;
    }

    @Override
    public void execute() {
        try (Connection ignored = dataSource.getConnection()) {
            LOG.debug("Database connection is alive");
        } catch (SQLException e) {
            LOG.error("Database connection failed. Evicting pool", e);
            HikariPoolMXBean poolMxBean = dataSource.getHikariPoolMXBean();
            if (poolMxBean != null) {
                poolMxBean.softEvictConnections();
            }
        }
    }

    @Override
    public String getName() {
        return "databaseReconnect";
    }
}
