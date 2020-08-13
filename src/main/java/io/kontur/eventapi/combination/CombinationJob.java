package io.kontur.eventapi.combination;

import io.kontur.eventapi.dao.KonturEventsDao;
import io.kontur.eventapi.dao.NormalizedObservationsDao;
import io.kontur.eventapi.dto.KonturEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class CombinationJob implements Runnable {

    public static final Logger LOG = LoggerFactory.getLogger(CombinationJob.class);
    private final NormalizedObservationsDao observationsDao;
    private final KonturEventsDao eventsDao;

    public CombinationJob(NormalizedObservationsDao observationsDao, KonturEventsDao eventsDao) {
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
        KonturEventDto newEventVersion = createDummyEventVersion(externalId);
        List<UUID> observations = observationsDao.getObservationIdsByExternalId(externalId);
        List<KonturEventDto> events = convertEvents(newEventVersion, observations);
        eventsDao.insertEventVersion(events);
    }

    private KonturEventDto createDummyEventVersion(String externalId) {
        return eventsDao.getLatestEventByExternalId(externalId)
                .map(event -> new KonturEventDto(event.getEventId(), event.getVersion() + 1))
                .orElseGet(() -> new KonturEventDto(UUID.randomUUID(), 1L));
    }

    private List<KonturEventDto> convertEvents(KonturEventDto event, List<UUID> observations) {
        if (observations.isEmpty()) {
            return Collections.emptyList();
        }
        return observations.stream()
                .map(obs -> new KonturEventDto(event.getEventId(), event.getVersion(), obs))
                .collect(Collectors.toList());
    }
}
