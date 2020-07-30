package io.kontur.eventapi.normalization;

import io.kontur.eventapi.dao.EventDataLakeDao;
import io.kontur.eventapi.dao.NormalizedRecordsDao;
import io.kontur.eventapi.dto.EventDataLakeDto;
import io.kontur.eventapi.dto.NormalizedRecordDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NormalizationJob implements Runnable {

    private final static Logger LOG = LoggerFactory.getLogger(NormalizationJob.class);

    private final List<Normalizer> normalizers;
    private final EventDataLakeDao eventDataLakeDao;
    private final NormalizedRecordsDao normalizedRecordsDao;

    public NormalizationJob(List<Normalizer> normalizers, EventDataLakeDao eventDataLakeDao,
                            NormalizedRecordsDao normalizedRecordsDao) {
        this.normalizers = normalizers;
        this.eventDataLakeDao = eventDataLakeDao;
        this.normalizedRecordsDao = normalizedRecordsDao;
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
                NormalizedRecordDto recordDto = normalizer.normalize(denormalizedEvent);
                normalizedRecordsDao.insertNormalizedRecords(recordDto);
                isNormalized = true;
                break;
            }
        }
        return isNormalized;
    }
}
