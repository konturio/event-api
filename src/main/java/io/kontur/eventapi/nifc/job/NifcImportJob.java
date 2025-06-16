package io.kontur.eventapi.nifc.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.nifc.client.NifcClient;
import io.kontur.eventapi.nifc.converter.NifcDataLakeConverter;
import io.micrometer.core.instrument.MeterRegistry;
import liquibase.repackaged.org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import reactor.util.function.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSONFactory;
import reactor.util.function.Tuples;

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
        int count = 0;
        count += processLocations();
        count += processPerimeters();
        updateObservationsMetric(count);
    }

    private int processLocations() {
        try {
            String data = client.getNifcLocations();
            try {
                return processFeatureCollection(data, NIFC_LOCATIONS_PROVIDER, "ModifiedOnDateTime_dt", "UniqueFireIdentifier");
            } catch (Exception e) {
                LOG.error("Failed to process NIFC locations", e);
            }
        } catch (Exception e) {
            LOG.warn("Failed to obtain NIFC locations");
        }
        return 0;
    }

    private int processPerimeters() {
        try {
            String data = client.getNifcPerimeters();
            try {
                return processFeatureCollection(data, NIFC_PERIMETERS_PROVIDER, "attr_ModifiedOnDateTime_dt", "attr_UniqueFireIdentifier");
            } catch (Exception e) {
                LOG.error("Failed to process NIFC perimeters");
            }
        } catch (Exception e) {
            LOG.warn("Failed to obtain NIFC perimeters");
        }
        return 0;
    }

    private int processFeatureCollection(String geoJson, String provider, String updatedAtProp, String externalIdProp) {
        try {
            FeatureCollection fc = (FeatureCollection) GeoJSONFactory.create(geoJson);
            Map<Tuple2<String, OffsetDateTime>, DataLake> dataLakes = new HashMap<>();
            Set<String> ids = Arrays.stream(fc.getFeatures())
                    .map(feature -> String.valueOf(feature.getProperties().get(externalIdProp)))
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toSet());
            Map<Tuple2<String, OffsetDateTime>, DataLake> existsDataLakes = new HashMap<>();
            dataLakeDao.getDataLakesByExternalIdsAndProvider(ids, provider)
                    .stream().filter(item -> StringUtils.isNotBlank(item.getExternalId()))
                    .forEach(dataLake -> existsDataLakes.put(
                            Tuples.of(dataLake.getExternalId(), dataLake.getUpdatedAt()), dataLake));
            for (Feature feature : fc.getFeatures()) {
                try {
                    String data = writeJson(feature);
                    String externalId = String.valueOf(feature.getProperties().get(externalIdProp));
                    long updatedAtMilli = Long.parseLong(String.valueOf(feature.getProperties().get(updatedAtProp)));
                    OffsetDateTime updatedAt = getDateTimeFromMilli(updatedAtMilli).truncatedTo(SECONDS);
                    if (!existsDataLakes.containsKey(Tuples.of(externalId, updatedAt))
                            && !dataLakes.containsKey(Tuples.of(externalId, updatedAt))) {
                        dataLakes.put(Tuples.of(externalId, updatedAt),
                                dataLakeConverter.convertDataLake(externalId, updatedAt, provider, data));
                    }
                } catch (Exception e) {
                    LOG.error("Failed to process feature from " + provider, e);
                }
            }
            if (MapUtils.isNotEmpty(dataLakes)) {
                dataLakeDao.storeDataLakes(dataLakes.values().stream().toList());
                return dataLakes.size();
            }
        } catch (Exception e) {
            LOG.error("Failed to process feature collection from " + provider, e);
        }
        return 0;
    }

    @Override
    public String getName() {
        return "nifcImport";
    }
}
