package io.kontur.eventapi.tornado.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.tornado.job.converter.TornadoDataLakeConverter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSONFactory;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.*;
import static io.kontur.eventapi.util.DateTimeUtil.parseDateTimeFromString;

@Component
public class StaticTornadoImportJob extends AbstractJob {

    private final static Logger LOG = LoggerFactory.getLogger(StaticTornadoImportJob.class);

    public final static String TORNADO_CANADA_GOV_PROVIDER = "tornado.canada-gov";
    public final static String TORNADO_AUSTRALIAN_BM_PROVIDER = "tornado.australian-bm";
    public final static String TORNADO_OSM_PROVIDER = "tornado.osm+wiki";

    private final static Map<String, String> PROVIDER_FILE_NAMES = Map.of(
            TORNADO_CANADA_GOV_PROVIDER, "static/kontur_tornado_pt_canada.json",
            TORNADO_AUSTRALIAN_BM_PROVIDER, "static/kontur_tornado_pt_australia.json",
            TORNADO_OSM_PROVIDER, "static/kontur_tornado_pt_osm.json"
    );

    private final static Map<String, OffsetDateTime> PROVIDER_UPDATE_DATES = Map.of(
            TORNADO_CANADA_GOV_PROVIDER, parseDateTimeFromString("16 Aug 2018 00:00:00 GMT"),
            TORNADO_AUSTRALIAN_BM_PROVIDER, parseDateTimeFromString("1 Jan 2020 00:00:00 GMT")
    );

    private final DataLakeDao dataLakeDao;
    private final TornadoDataLakeConverter converter;

    protected StaticTornadoImportJob(MeterRegistry meterRegistry, DataLakeDao dataLakeDao,
                                     TornadoDataLakeConverter converter) {
        super(meterRegistry);
        this.dataLakeDao = dataLakeDao;
        this.converter = converter;
    }

    @Override
    public void execute() throws Exception {
        PROVIDER_FILE_NAMES.forEach(this::processGeoJSON);
    }

    @Override
    public String getName() {
        return "staticTornadoImport";
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
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
        if (inputStream == null) {
            throw new Exception("File not found: " + fileName);
        }
        return new String(inputStream.readAllBytes());
    }

    private List<DataLake> createDateLakes(Feature[] features, String provider) {
        List<DataLake> dataLakes = new ArrayList<>();

        for (Feature feature: features) {
            String externalId = DigestUtils.md5Hex(feature.toString());
            if (dataLakeDao.getDataLakeByExternalIdAndProvider(externalId, provider).isEmpty()) {
                OffsetDateTime updatedAt = PROVIDER_UPDATE_DATES.getOrDefault(provider, null);
                DataLake dataLake = converter.convertDataLake(externalId, updatedAt, provider, feature.toString());
                dataLakes.add(dataLake);
            }
        }
        return dataLakes;
    }
}
