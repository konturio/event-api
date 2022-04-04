package io.kontur.eventapi.firms.jobs;

import static io.kontur.eventapi.firms.FirmsUtil.MODIS_PROVIDER;
import java.util.List;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.firms.client.FirmsClient;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class FirmsImportModisJob extends FirmsImportJob {

    public FirmsImportModisJob(FirmsClient firmsClient, DataLakeDao dataLakeDao, MeterRegistry meterRegistry) {
        super(firmsClient, dataLakeDao, meterRegistry);
    }

    @Override
    protected List<DataLake> loadData() {
        String modisData = firmsClient.getModisData();
        Counter counter = meterRegistry.counter("import.firms.modis.counter");
        return createDataLakes(MODIS_PROVIDER, modisData, counter);
    }

    @Override
    public String getName() {
        return "firmsModisImport";
    }

}
