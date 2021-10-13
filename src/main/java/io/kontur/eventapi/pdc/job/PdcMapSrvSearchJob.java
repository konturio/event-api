package io.kontur.eventapi.pdc.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.pdc.client.PdcMapSrvClient;
import io.kontur.eventapi.pdc.converter.PdcDataLakeConverter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSONFactory;

import java.util.*;

@Component
public class PdcMapSrvSearchJob extends AbstractJob {

    private final Logger LOG = LoggerFactory.getLogger(PdcMapSrvSearchJob.class);
    private final PdcMapSrvClient pdcMapSrvClient;
    private final PdcDataLakeConverter pdcDataLakeConverter;
    private final DataLakeDao dataLakeDao;

    public PdcMapSrvSearchJob(MeterRegistry meterRegistry, PdcMapSrvClient pdcMapSrvClient,
                              PdcDataLakeConverter pdcDataLakeConverter, DataLakeDao dataLakeDao) {
        super(meterRegistry);
        this.pdcMapSrvClient = pdcMapSrvClient;
        this.pdcDataLakeConverter = pdcDataLakeConverter;
        this.dataLakeDao = dataLakeDao;
    }

    @Override
    public String getName() {
        return "pdcMapSrv";
    }

    @Override
    public void execute() throws Exception {
        try {
            String geoJson = pdcMapSrvClient.getExposures();
            FeatureCollection featureCollection = (FeatureCollection) GeoJSONFactory.create(geoJson);
            List<DataLake> dataLakes = new ArrayList<>();
            for (Feature feature : featureCollection.getFeatures()) {
                String externalId = String.valueOf(feature.getProperties().get("hazard_uuid"));
                String geoHash = (String) feature.getProperties().get("geohash");
                if (geoHash != null && dataLakeDao.isNewPdcExposure(externalId, geoHash)) {
                    dataLakes.add(pdcDataLakeConverter.convertExposure(feature.toString(), externalId));
                }
            }
            dataLakeDao.storeDataLakes(dataLakes);
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
    }
}
