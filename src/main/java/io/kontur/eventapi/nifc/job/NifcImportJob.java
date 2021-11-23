package io.kontur.eventapi.nifc.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.nifc.client.NifcClient;
import io.kontur.eventapi.nifc.converter.NifcDataLakeConverter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSONFactory;

import java.time.OffsetDateTime;
import java.util.*;

import static io.kontur.eventapi.nifc.converter.NifcDataLakeConverter.NIFC_LOCATIONS_PROVIDER;
import static io.kontur.eventapi.nifc.converter.NifcDataLakeConverter.NIFC_PERIMETERS_PROVIDER;
import static io.kontur.eventapi.util.DateTimeUtil.getDateTimeFromMilli;
import static io.kontur.eventapi.util.JsonUtil.writeJson;

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
            processFeatureCollection(client.getNifcLocations(), "ModifiedOnDateTime_dt", NIFC_LOCATIONS_PROVIDER);
        } catch (Exception e) {
            LOG.error("Failed to obtain NIFC locations");
        }
    }

    private void processPerimeters() {
        try {
            processFeatureCollection(client.getNifcPerimeters(), "irwin_ModifiedOnDateTime_dt", NIFC_PERIMETERS_PROVIDER);
        } catch (Exception e) {
            LOG.error("Failed to obtain NIFC perimeters");
        }
    }

    private void processFeatureCollection(String geoJson, String updatedAtProp, String provider) {
        try {
            FeatureCollection fc = (FeatureCollection) GeoJSONFactory.create(geoJson);
            List<DataLake> dataLakes = new ArrayList<>();
            for (Feature feature : fc.getFeatures()) {
                try {
                    String data = writeJson(feature);
                    String externalId = DigestUtils.md5Hex(data);
                    long updatedAtMilli = Long.parseLong(String.valueOf(feature.getProperties().get(updatedAtProp)));
                    OffsetDateTime updatedAt = getDateTimeFromMilli(updatedAtMilli);
                    if (dataLakeDao.getLatestDataLakeByExternalIdAndProvider(externalId, provider).isEmpty()) {
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
