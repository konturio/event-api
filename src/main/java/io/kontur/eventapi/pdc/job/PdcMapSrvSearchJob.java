package io.kontur.eventapi.pdc.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.pdc.client.PdcMapSrvClient;
import io.kontur.eventapi.pdc.converter.PdcDataLakeConverter;
import io.kontur.eventapi.entity.ExposureGeohash;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSONFactory;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

    public Boolean run(String serviceId) {
        String jobName = this.getClass().getName() + serviceId;

        getLocks().putIfAbsent(jobName, new ReentrantLock());
        Lock lock = getLocks().get(jobName);

        if (!lock.tryLock()) {
            printThreadDump();
            throw new IllegalStateException("Parallel execution of same job is not supported, job" + jobName);
        }

        LOG.debug("PDC MapSrv {} job has started", serviceId);
        io.micrometer.core.instrument.Timer.Sample regularTimer = Timer.start(getMeterRegistry());
        LongTaskTimer.Sample longTaskTimer = LongTaskTimer.builder("job." + getName() + serviceId + ".current")
                .register(getMeterRegistry()).start();

        try {
            execute(serviceId);
        } catch (Exception e) {
            long duration = stopTimer(regularTimer);
            longTaskTimer.stop();
            LOG.error("PDC MapSrv {} job has failed after {} seconds", serviceId, duration, e);
            throw new RuntimeException("failed job " + jobName, e);
        } finally {
            lock.unlock();
        }

        long duration = stopTimer(regularTimer);
        longTaskTimer.stop();
        LOG.debug("PDC MapSrv {} job has finished in {} seconds", serviceId, duration);
        if (duration > 60) {
            LOG.warn("[slow_job] {} seconds (PDC MapSrv {})", duration, serviceId);
        }
        return Boolean.TRUE;
    }

    @Override
    public void execute() throws Exception {
        throw new RuntimeException("PDC MapSrv used wrong execution function");
    }

    @Override
    public String getName() {
        return "pdcMapSrvImport";
    }

    public void execute(String serviceId) throws Exception {
        try {
            String geoJson = pdcMapSrvClient.getTypeSpecificExposures(serviceId);
            if (geoJson == null || geoJson.isBlank()) {
                LOG.warn("Received empty response from PDC MapSrv {}", serviceId);
                return;
            }
            FeatureCollection featureCollection = (FeatureCollection) GeoJSONFactory.create(geoJson);
            List<DataLake> dataLakes = new ArrayList<>();
            Map<String, String> ids = new HashMap<>();
            Map<String, String> features = new HashMap<>();
            for (Feature feature : featureCollection.getFeatures()) {
                String externalId = String.valueOf(feature.getProperties().get("hazard_uuid"));
                String geoHash = (String) feature.getProperties().get("geohash");
                if (geoHash != null) {
                    ids.put(externalId, geoHash);
                    features.put(externalId, feature.toString());
                }
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
                    }
                }
                updateObservationsMetric(dataLakes.size());
                if (!CollectionUtils.isEmpty(dataLakes)) {
                    dataLakeDao.storeDataLakes(dataLakes);
                }
            }
        } catch (Exception e) {
            LOG.warn("Exposures wasn't received from PDC MapSrv {}. Error: {}", serviceId, e.getMessage());
        }
    }
}
