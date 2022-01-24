package io.kontur.eventapi.nifc.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.nifc.client.NifcClient;
import io.kontur.eventapi.nifc.converter.NifcDataLakeConverter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSONFactory;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static io.kontur.eventapi.nifc.converter.NifcDataLakeConverter.NIFC_LOCATIONS_PROVIDER;
import static io.kontur.eventapi.nifc.converter.NifcDataLakeConverter.NIFC_PERIMETERS_PROVIDER;
import static io.kontur.eventapi.util.DateTimeUtil.getDateTimeFromMilli;
import static io.kontur.eventapi.util.JsonUtil.writeJson;
import static java.time.temporal.ChronoUnit.SECONDS;

@Component
public class NifcImportJob extends AbstractJob {

    private static final Logger LOG = LoggerFactory.getLogger(NifcImportJob.class);
    private final NifcClient client;
    private final DataLakeDao dataLakeDao;
    private final NifcDataLakeConverter dataLakeConverter;

    protected NifcImportJob(MeterRegistry meterRegistry, NifcClient client, DataLakeDao dataLakeDao,
                            NifcDataLakeConverter dataLakeConverter) {
        super(meterRegistry);
        this.client = client;
        this.dataLakeDao = dataLakeDao;
        this.dataLakeConverter = dataLakeConverter;
    }

    @Override
    public void execute() throws Exception {
        processLocations();
        processPerimeters();
    }

    private void processLocations() {
        try {
            processFeatureCollection(client.getNifcLocations(), NIFC_LOCATIONS_PROVIDER,
                    "ModifiedOnDateTime_dt", "UniqueFireIdentifier");
        } catch (Exception e) {
            LOG.error("Failed to obtain NIFC locations");
        }
    }

    private void processPerimeters() {
        try {
            processFeatureCollection(client.getNifcPerimeters(), NIFC_PERIMETERS_PROVIDER,
                    "irwin_ModifiedOnDateTime_dt", "irwin_UniqueFireIdentifier");
        } catch (Exception e) {
            LOG.error("Failed to obtain NIFC perimeters");
        }
    }

    private void processFeatureCollection(String geoJson, String provider, String updatedAtProp, String externalIdProp) {
        try {
            FeatureCollection fc = (FeatureCollection) GeoJSONFactory.create(geoJson);
            List<DataLake> dataLakes = new ArrayList<>();
            Set<String> ids = Arrays.stream(fc.getFeatures())
                    .map(feature -> String.valueOf(feature.getProperties().get(externalIdProp)))
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toSet());
            Map<String, DataLake> existsDataLakes = new HashMap<>();
            try {
                dataLakeDao.getDataLakesByExternalIdsAndProvider(ids, provider)
                        .forEach(dataLake -> existsDataLakes.put(dataLake.getExternalId(), dataLake));
            } catch (Exception e) {
                LOG.error("Failed to get exists DataLakes for " + provider, e);
            }
            for (Feature feature : fc.getFeatures()) {
                try {
                    String data = writeJson(feature);
                    String externalId = String.valueOf(feature.getProperties().get(externalIdProp));
                    long updatedAtMilli = Long.parseLong(String.valueOf(feature.getProperties().get(updatedAtProp)));
                    OffsetDateTime updatedAt = getDateTimeFromMilli(updatedAtMilli).truncatedTo(SECONDS);
                    if (!existsDataLakes.containsKey(externalId)
                            || !existsDataLakes.get(externalId).getUpdatedAt().isEqual(updatedAt)) {
                        dataLakes.add(dataLakeConverter.convertDataLake(externalId, updatedAt, provider, data));
                    }
                } catch (Exception e) {
                    LOG.error("Failed to process feature from " + provider, e);
                }
            }
            dataLakeDao.storeDataLakes(dataLakes);
        } catch (Exception e) {
            LOG.error("Failed to process feature collection from " + provider, e);
        }

    }

    @Override
    public String getName() {
        return "nifcImport";
    }
}
