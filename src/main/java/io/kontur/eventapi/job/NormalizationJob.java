package io.kontur.eventapi.job;

import io.kontur.eventapi.dao.EventDataLakeDao;
import io.kontur.eventapi.dao.NormalizedObservationsDao;
import io.kontur.eventapi.dto.EventDataLakeDto;
import io.kontur.eventapi.dto.NormalizedObservationsDto;
import io.kontur.eventapi.normalization.Normalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NormalizationJob implements Runnable {

    private final static Logger LOG = LoggerFactory.getLogger(NormalizationJob.class);

    private final List<Normalizer> normalizers;
    private final EventDataLakeDao eventDataLakeDao;
    private final NormalizedObservationsDao normalizedObservationsDao;

    public NormalizationJob(List<Normalizer> normalizers, EventDataLakeDao eventDataLakeDao,
                            NormalizedObservationsDao normalizedObservationsDao) {
        this.normalizers = normalizers;
        this.eventDataLakeDao = eventDataLakeDao;
        this.normalizedObservationsDao = normalizedObservationsDao;
    }

    @Override
    public void run() {
        List<EventDataLakeDto> denormalizedEvents = eventDataLakeDao.getDenormalizedEvents();
        LOG.info("Normalization job has started. Events to process: {}", denormalizedEvents.size());

        for (EventDataLakeDto denormalizedEvent : denormalizedEvents) {
            boolean isNormalized = normalize(denormalizedEvent);
            if (!isNormalized) {
                LOG.info("Event wasn't normalized. Provider: {}, observation: {}", denormalizedEvent.getProvider(),
                        denormalizedEvent.getObservationId());
            }
        }

        LOG.info("Normalization job has finished");
    }

    private boolean normalize(EventDataLakeDto denormalizedEvent) {
        boolean isNormalized = false;
        for (Normalizer normalizer : normalizers) {
            if (normalizer.isApplicable(denormalizedEvent)) {
                NormalizedObservationsDto normalizedDto = normalizer.normalize(denormalizedEvent);
                normalizedObservationsDao.insert(normalizedDto);
                isNormalized = true;
                break;
            }
        }
        return isNormalized;
    }
}
