package io.kontur.eventapi.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.dao.NormalizedObservationsDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.normalization.Normalizer;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NormalizationJob implements Runnable {

    private final static Logger LOG = LoggerFactory.getLogger(NormalizationJob.class);

    private final List<Normalizer> normalizers;
    private final DataLakeDao dataLakeDao;
    private final NormalizedObservationsDao normalizedObservationsDao;

    public NormalizationJob(List<Normalizer> normalizers, DataLakeDao dataLakeDao,
                            NormalizedObservationsDao normalizedObservationsDao) {
        this.normalizers = normalizers;
        this.dataLakeDao = dataLakeDao;
        this.normalizedObservationsDao = normalizedObservationsDao;
    }

    @Override
    @Timed(value = "job.normalization")
    public void run() {
        List<DataLake> denormalizedEvents = dataLakeDao.getDenormalizedEvents();
        LOG.info("Normalization job has started. Events to process: {}", denormalizedEvents.size());

        for (DataLake denormalizedEvent : denormalizedEvents) {
            boolean isNormalized = normalize(denormalizedEvent);
            if (!isNormalized) {
                LOG.info("Event wasn't normalized. Provider: {}, observation: {}", denormalizedEvent.getProvider(),
                        denormalizedEvent.getObservationId());
            }
        }

        LOG.info("Normalization job has finished");
    }

    private boolean normalize(DataLake denormalizedEvent) {
        boolean isNormalized = false;
        for (Normalizer normalizer : normalizers) {
            if (normalizer.isApplicable(denormalizedEvent)) {
                try {
                    NormalizedObservation normalizedDto = normalizer.normalize(denormalizedEvent);
                    normalizedObservationsDao.insert(normalizedDto);
                    isNormalized = true;
                } catch (Exception e) {
                    LOG.warn(e.getMessage(), e);
                }
                break;
            }
        }
        return isNormalized;
    }
}
