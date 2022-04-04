package io.kontur.eventapi.pdc.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.pdc.client.PdcMapSrvClient;
import io.kontur.eventapi.pdc.converter.PdcDataLakeConverter;
import io.kontur.eventapi.entity.ExposureGeohash;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
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
        return "pdcMapSrvImport";
    }

    @Override
    public void execute() throws Exception {
        try {
            Counter counter = meterRegistry.counter("import.pdc.exposure.counter");
            String geoJson = pdcMapSrvClient.getExposures();
            FeatureCollection featureCollection = (FeatureCollection) GeoJSONFactory.create(geoJson);
            List<DataLake> dataLakes = new ArrayList<>();
            Map<String, String> ids = new HashMap<>();
            Map<String, String> features = new HashMap<>();
            for (Feature feature : featureCollection.getFeatures()) {
                String externalId = String.valueOf(feature.getProperties().get("hazard_uuid"));
                String geoHash = (String) feature.getProperties().get("geohash");
                ids.put(externalId, geoHash);
                features.put(externalId, feature.toString());
            }
            if (!ids.isEmpty()) {
                List<ExposureGeohash> exposureGeohashes = dataLakeDao.getPdcExposureGeohashes(ids.keySet());
                Map<String, ExposureGeohash> storedValues = new HashMap<>();
                exposureGeohashes.forEach(value -> storedValues.put(value.getExternalId(), value));
                for (String id : ids.keySet()) {
                    if ((!storedValues.containsKey(id) || StringUtils.isBlank(storedValues.get(id).getGeohash())
                            || !storedValues.get(id).getGeohash().equals(ids.get(id)))
                            && dataLakes.stream().noneMatch(i -> i.getExternalId().equals(id))) {
                        dataLakes.add(pdcDataLakeConverter.convertExposure(features.get(id), id));
                        counter.increment();
                    }
                }
                if (!CollectionUtils.isEmpty(dataLakes)) {
                    dataLakeDao.storeDataLakes(dataLakes);
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
    }
}
