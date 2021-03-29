package io.kontur.eventapi.tornado.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.tornado.converter.TornadoDataLakeConverter;
import io.kontur.eventapi.tornado.service.StaticTornadoImportService;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import java.time.OffsetDateTime;
import java.util.*;

@Component
public class StaticTornadoImportJob extends AbstractJob {

    private final DataLakeDao dataLakeDao;
    private final TornadoDataLakeConverter tornadoDataLakeConverter;
    private final StaticTornadoImportService staticTornadoImportService;

    public final static String TORNADO_CANADA_GOV_PROVIDER = "tornado.canada-gov";
    public final static String TORNADO_AUSTRALIAN_BM_PROVIDER = "tornado.australian-bm";
    public final static String TORNADO_OSM_PROVIDER = "tornado.osm+wiki";

    protected StaticTornadoImportJob(MeterRegistry meterRegistry, StaticTornadoImportService staticTornadoImportService,
                                     DataLakeDao dataLakeDao, TornadoDataLakeConverter tornadoDataLakeConverter) {
        super(meterRegistry);
        this.dataLakeDao = dataLakeDao;
        this.tornadoDataLakeConverter = tornadoDataLakeConverter;
        this.staticTornadoImportService = staticTornadoImportService;
    }

    @Override
    public void execute() throws Exception {
        Map<String, String> providerFilenames = staticTornadoImportService.getProviderFilenames();
        for (var providerFilename: providerFilenames.entrySet()) {
            String json = staticTornadoImportService.readFile(providerFilename.getValue());
            Feature[] features = staticTornadoImportService.getFeatures(json);
            createDateLakes(features, providerFilename.getKey());
        }
    }

    @Override
    public String getName() {
        return "staticTornadoImport";
    }

    private void createDateLakes(Feature[] features, String provider) {
        List<DataLake> dataLakes = new ArrayList<>();
        for (Feature feature: features) {
            String externalId = DigestUtils.md5Hex(feature.toString());
            if (dataLakeDao.getDataLakesByExternalId(externalId).isEmpty()) {
                OffsetDateTime updatedAt = staticTornadoImportService.getProviderUpdateDate(provider);
                DataLake dataLake = tornadoDataLakeConverter.convert(externalId, updatedAt, provider, feature.toString());
                dataLakes.add(dataLake);
            }
        }
        dataLakeDao.storeDataLakes(dataLakes);
    }
}
