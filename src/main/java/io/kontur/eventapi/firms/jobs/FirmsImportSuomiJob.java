package io.kontur.eventapi.firms.jobs;

import static io.kontur.eventapi.firms.FirmsUtil.SUOMI_PROVIDER;

import java.util.List;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.firms.client.FirmsClient;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class FirmsImportSuomiJob extends FirmsImportJob {

    public FirmsImportSuomiJob(FirmsClient firmsClient, DataLakeDao dataLakeDao, MeterRegistry meterRegistry) {
        super(firmsClient, dataLakeDao, meterRegistry);
    }

    @Override
    protected List<DataLake> loadData() {
        String suomiData = firmsClient.getSuomiNppVirsData();
        return createDataLakes(SUOMI_PROVIDER, suomiData);
    }

    @Override
    public String getName() {
        return "firmsSuomiImport";
    }

}
