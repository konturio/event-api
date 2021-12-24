package io.kontur.eventapi.metrics.collector;

import io.kontur.eventapi.dao.GeneralDao;
import io.kontur.eventapi.entity.PgSetting;
import io.kontur.eventapi.entity.PgStatTable;
import io.kontur.eventapi.metrics.MetricCollector;
import io.kontur.eventapi.metrics.config.TableMetricsConfig;
import org.springframework.stereotype.Component;

import java.util.Map;

import static java.lang.Double.parseDouble;

@Component
public class AutovacuumMetricsCollector implements MetricCollector {

    private final Map<String, TableMetricsConfig> tableMetrics;
    private final GeneralDao generalDao;

    public AutovacuumMetricsCollector(Map<String, TableMetricsConfig> tableMetrics, GeneralDao generalDao) {
        this.tableMetrics = tableMetrics;
        this.generalDao = generalDao;
    }

    @Override
    public void collect() {
        Map<String, PgStatTable> pgStatTables = generalDao.getPgStatTables();
        Map<String, PgSetting> pgSettings = generalDao.getPgSettings();

        tableMetrics.forEach((key, value) -> {
            PgStatTable pgStatTable = pgStatTables.get(key);
            value.getVacuumCount().set(checkNotNull(pgStatTable.getVacuumCount()));
            value.getAutovacuumCount().set(checkNotNull(pgStatTable.getAutoVacuumCount()));
            value.getAnalyseCount().set(checkNotNull(pgStatTable.getAnalyzeCount()));
            value.getAutoAnalyseCount().set(checkNotNull(pgStatTable.getAutoAnalyzeCount()));
            value.getAutovacuumConditionActualValue().set(checkNotNull(pgStatTable.getDeadTupCount()));

            double autovacuumScaleFactor = parseDouble(pgSettings.get("autovacuum_vacuum_scale_factor").getSetting());
            double autovacuumThreshold = parseDouble(pgSettings.get("autovacuum_vacuum_threshold").getSetting());

            value.getAutovacuumConditionExpectedValue()
                    .set(autovacuumThreshold + autovacuumScaleFactor * checkNotNull(pgStatTable.getLiveTupCount()));
        });
    }

    private Long checkNotNull(Long value) {
        return value == null ? 0L : value;
    }
}
