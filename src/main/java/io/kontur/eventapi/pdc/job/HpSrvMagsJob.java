package io.kontur.eventapi.pdc.job;

import com.fasterxml.jackson.databind.JsonNode;
import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.pdc.service.HpSrvService;
import io.kontur.eventapi.util.JsonUtil;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HpSrvMagsJob extends AbstractJob {

    private final static Logger LOG = LoggerFactory.getLogger(HpSrvMagsJob.class);

    private final DataLakeDao dataLakeDao;
    private final HpSrvService hpSrvService;

    @Autowired
    public HpSrvMagsJob(DataLakeDao dataLakeDao, HpSrvService hpSrvService, MeterRegistry meterRegistry) {
        super(meterRegistry);
        this.dataLakeDao = dataLakeDao;
        this.hpSrvService = hpSrvService;
    }

    @Override
    public void execute() {
        importMags();
    }

    @Override
    public String getName() {
        return "hpSrvMagImport";
    }

    private void importMags() {
        Counter counter = meterRegistry.counter("import.pdc.mags.counter");
        List<DataLake> eventsWithoutAreas = dataLakeDao.getPdcHpSrvHazardsWithoutAreas();
        LOG.info("{} hazards to process", eventsWithoutAreas.size());

        for (int i = 0; i < eventsWithoutAreas.size(); i++) {
            if ((eventsWithoutAreas.size() - i) % 100 == 0) {
                LOG.info("{} hazards to process", eventsWithoutAreas.size() - i);
            }

            DataLake dataLake = eventsWithoutAreas.get(i);
            String externalId = dataLake.getExternalId();
            String hazardId = getHazardId(dataLake);
            try {
                JsonNode json = hpSrvService.obtainMagsFeatureCollection(hazardId);
                hpSrvService.saveMag(externalId, json, counter);
            } catch (Exception e) {
                LOG.warn("Exception during hazard mag processing. Hazard UUID = '{}'", externalId, e);
            }
        }
    }

    private String getHazardId(DataLake dataLake) {
        JsonNode jsonNode = JsonUtil.readTree(dataLake.getData());
        return jsonNode.get("hazard_ID").asText();
    }
}
