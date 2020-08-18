package io.kontur.eventapi.job;

import io.kontur.eventapi.dao.KonturEventsDao;
import io.kontur.eventapi.dao.NormalizedObservationsDao;
import io.kontur.eventapi.dto.KonturEventDto;
import io.kontur.eventapi.dto.NormalizedObservationsDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

@Component
public class EventCombinationJob implements Runnable {

    public static final Logger LOG = LoggerFactory.getLogger(EventCombinationJob.class);
    private final NormalizedObservationsDao observationsDao;
    private final KonturEventsDao eventsDao;

    public EventCombinationJob(NormalizedObservationsDao observationsDao, KonturEventsDao eventsDao) {
        this.observationsDao = observationsDao;
        this.eventsDao = eventsDao;
    }

    @Override
    public void run() {
        List<String> externalIds = observationsDao.getExternalIdsToUpdate();

        LOG.info("Combination job has started. Events to process: {}", externalIds.size());

        for (String externalId : externalIds) {
            processEvent(externalId);
        }

        LOG.info("Combination job has finished");
    }

    private void processEvent(String externalId) {
        KonturEventDto newEventVersion = createNewEventVersion(externalId);
        List<UUID> observations = observationsDao.getObservationsByExternalId(externalId)
                .stream()
                .map(NormalizedObservationsDto::getObservationId)
                .collect(toList());
        newEventVersion.addObservations(observations);
        eventsDao.insertEventVersion(newEventVersion);
    }

    private KonturEventDto createNewEventVersion(String externalId) {
        return eventsDao.getLatestEventByExternalId(externalId)
                .map(event -> new KonturEventDto(event.getEventId(), event.getVersion() + 1))
                .orElseGet(() -> new KonturEventDto(UUID.randomUUID(), 1L));
    }
}
