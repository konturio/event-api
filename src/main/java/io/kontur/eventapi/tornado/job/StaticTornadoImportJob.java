package io.kontur.eventapi.tornado.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.util.DateTimeUtil;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSONFactory;
import java.io.InputStream;
import java.util.*;

@Component
public class StaticTornadoImportJob extends AbstractJob {

    private final static Logger LOG = LoggerFactory.getLogger(StaticTornadoImportJob.class);

    public final static String CANADA_GOV = "canada-gov";
    public final static String AUSTRALIAN_BM = "australian-bm";

    private final static Map<String, String> PROVIDERS = Map.of(
            CANADA_GOV, "static/kontur_tornado_pt_canada.json",
            AUSTRALIAN_BM, "static/kontur_tornado_pt_australia.json"
    );

    private final DataLakeDao dataLakeDao;

    protected StaticTornadoImportJob(MeterRegistry meterRegistry, DataLakeDao dataLakeDao) {
        super(meterRegistry);
        this.dataLakeDao = dataLakeDao;
    }

    @Override
    public void execute() throws Exception {
        PROVIDERS.forEach(this::processGeoJSON);
    }

    @Override
    public String getName() {
        return "canadaGovImport";
    }

    private void processGeoJSON(String provider, String fileName) {
        try {
            String json = readFile(fileName);
            FeatureCollection featureCollection = (FeatureCollection) GeoJSONFactory.create(json);
            List<DataLake> dataLakes = createDateLakes(featureCollection.getFeatures(), provider);
            dataLakeDao.storeDataLakes(dataLakes);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private String readFile(String fileName) throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);
        if (inputStream == null) {
            throw new Exception("File not found: " + fileName);
        }
        return new String(inputStream.readAllBytes());
    }

    private List<DataLake> createDateLakes(Feature[] features, String provider) {
        List<DataLake> dataLakes = new ArrayList<>();
        for (Feature feature: features) {
            DataLake dataLake = new DataLake();
            dataLake.setObservationId(UUID.randomUUID());
            dataLake.setExternalId(UUID.randomUUID().toString());
            dataLake.setData(feature.toString());
            dataLake.setProvider(provider);
            dataLake.setLoadedAt(DateTimeUtil.uniqueOffsetDateTime());
            dataLakes.add(dataLake);
        }
        return dataLakes;
    }
}
