package io.kontur.eventapi.firms.jobs;

import static io.kontur.eventapi.firms.FirmsUtil.NOAA_PROVIDER;

import java.util.List;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.firms.client.FirmsClient;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class FirmsImportNoaaJob extends FirmsImportJob {

    public FirmsImportNoaaJob(FirmsClient firmsClient, DataLakeDao dataLakeDao, MeterRegistry meterRegistry) {
        super(firmsClient, dataLakeDao, meterRegistry);
    }

    @Override
    protected List<DataLake> loadData() {
        String noaaData = firmsClient.getNoaa20VirsData();
        Counter counter = meterRegistry.counter("import.firms.noaa.counter");
        return createDataLakes(NOAA_PROVIDER, noaaData, counter);
    }

    @Override
    public String getName() {
        return "firmsNoaaImport";
    }

}
