package io.kontur.eventapi.usgs.earthquake.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.dao.NormalizedObservationsDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.usgs.earthquake.converter.UsgsEarthquakeDataLakeConverter;
import io.kontur.eventapi.usgs.earthquake.normalization.UsgsEarthquakeNormalizer;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Component
public class UsgsEarthquakeNormalizationJob extends AbstractJob {

    private static final Logger LOG = LoggerFactory.getLogger(UsgsEarthquakeNormalizationJob.class);
    private final UsgsEarthquakeNormalizer normalizer;
    private final DataLakeDao dataLakeDao;
    private final NormalizedObservationsDao observationsDao;

    public UsgsEarthquakeNormalizationJob(MeterRegistry meterRegistry,
                                          UsgsEarthquakeNormalizer normalizer,
                                          DataLakeDao dataLakeDao,
                                          NormalizedObservationsDao observationsDao) {
        super(meterRegistry);
        this.normalizer = normalizer;
        this.dataLakeDao = dataLakeDao;
        this.observationsDao = observationsDao;
    }

    @Override
    public String getName() {
        return "usgsEarthquakeNormalization";
    }

    @Override
    public void execute() {
        List<DataLake> dataLakes = dataLakeDao.getDenormalizedEvents(
                List.of(UsgsEarthquakeDataLakeConverter.USGS_EARTHQUAKE_PROVIDER));
        if (CollectionUtils.isEmpty(dataLakes)) {
            LOG.debug("No USGS earthquake data to normalize");
            return;
        }

        LOG.info("USGS normalization: {} data lakes", dataLakes.size());
        for (DataLake dl : dataLakes) {
            try {
                LOG.debug("Normalizing USGS earthquake {} at {}", dl.getExternalId(), dl.getUpdatedAt());
                if (!normalizer.isSkipped()) {
                    observationsDao.insert(normalizer.normalize(dl));
                    LOG.info("USGS earthquake {} normalized", dl.getExternalId());
                } else {
                    dataLakeDao.markAsSkipped(dl.getObservationId());
                    LOG.info("USGS earthquake {} skipped", dl.getExternalId());
                }
            } catch (Exception e) {
                LOG.warn("Failed to normalize USGS earthquake {}", dl.getExternalId(), e);
            }
        }
    }
}
