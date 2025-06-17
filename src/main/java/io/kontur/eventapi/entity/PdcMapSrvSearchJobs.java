package io.kontur.eventapi.entity;

import java.util.ArrayList;
import java.util.List;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.pdc.client.PdcMapSrvClient;
import io.kontur.eventapi.pdc.converter.PdcDataLakeConverter;
import io.kontur.eventapi.pdc.job.PdcMapSrvSearchJob;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class PdcMapSrvSearchJobs {
    public static final String[] PDC_MAP_SRV_IDS
            = new String[] {"0", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11",
            "13", "14", "15", "16", "17", "20", "25",
            "32", "33", "34", "35", "36", "37", "38", "39", "42"};

    private final List<PdcMapSrvSearchJob> jobs;

    public PdcMapSrvSearchJobs(MeterRegistry meterRegistry, PdcMapSrvClient pdcMapSrvClient,
                               PdcDataLakeConverter pdcDataLakeConverter, DataLakeDao dataLakeDao) {
        jobs = new ArrayList<>();
        for (int i = 0; i < PDC_MAP_SRV_IDS.length; i++) {
            PdcMapSrvSearchJob job =
                    new PdcMapSrvSearchJob(meterRegistry, pdcMapSrvClient, pdcDataLakeConverter, dataLakeDao);
            jobs.add(job);
        }
    }

    public List<PdcMapSrvSearchJob> getJobs() {
        return jobs;
    }
}
