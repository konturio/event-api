package io.kontur.eventapi.job;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPoolMXBean;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

public class DatabaseReconnectJobTest {

    @Test
    public void shouldEvictPoolOnFailure() throws SQLException {
        HikariDataSource dataSource = mock(HikariDataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("fail"));
        HikariPoolMXBean mxBean = mock(HikariPoolMXBean.class);
        when(dataSource.getHikariPoolMXBean()).thenReturn(mxBean);

        DatabaseReconnectJob job = new DatabaseReconnectJob(new SimpleMeterRegistry(), dataSource);
        assertDoesNotThrow(job::run);

        verify(mxBean, times(1)).softEvictConnections();
    }

    @Test
    public void shouldNotEvictPoolWhenConnectionOk() throws Exception {
        HikariDataSource dataSource = mock(HikariDataSource.class);
        HikariPoolMXBean mxBean = mock(HikariPoolMXBean.class);
        when(dataSource.getHikariPoolMXBean()).thenReturn(mxBean);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);

        DatabaseReconnectJob job = new DatabaseReconnectJob(new SimpleMeterRegistry(), dataSource);
        job.run();

        verify(mxBean, never()).softEvictConnections();
        verify(connection, times(1)).close();
    }
}
