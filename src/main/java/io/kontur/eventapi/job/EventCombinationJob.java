package io.kontur.eventapi.job;

import io.kontur.eventapi.dao.KonturEventsDao;
import io.kontur.eventapi.dao.NormalizedObservationsDao;
import io.kontur.eventapi.entity.KonturEvent;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Comparator;
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
    @Timed("job.eventCombination")
    public void run() {
        List<String> externalIds = observationsDao.getExternalIdsToUpdate();

        LOG.info("Combination job has started. Events to process: {}", externalIds.size());
        externalIds.forEach(this::processEvent);
        LOG.info("Combination job has finished");

    }

    private void processEvent(String externalId) {
        var normalizedObservations = observationsDao.getObservationsByExternalId(externalId);
        var newEventVersion = createNewEventVersion(externalId);

        List<NormalizedObservation> filteredObservations = normalizedObservations.stream()
                .filter(obs -> newEventVersion.getObservationIds().stream()
                        .noneMatch(id -> obs.getObservationId().equals(id)))
                .sorted(Comparator.comparing(NormalizedObservation::getLoadedAt))
                .collect(toList());

        if (!filteredObservations.isEmpty()) {
            var loadedDate = filteredObservations.get(0).getLoadedAt();
            for (NormalizedObservation observation : filteredObservations) {
                if (loadedDate.plusMinutes(1).isAfter(observation.getLoadedAt())) {
                    try {
                        newEventVersion.addObservations(observation.getObservationId());
                    } catch (Exception e) {
                        LOG.warn("observationID = {}", observation.getObservationId());
                        LOG.warn("observations in event = {}", newEventVersion.getObservationIds());
                        LOG.warn(e.getLocalizedMessage());
                    }

                }
            }
            eventsDao.insertEventVersion(newEventVersion);
        }
    }

    private KonturEvent createNewEventVersion(String externalId) {
        var eventOptional = eventsDao.getLatestEventByExternalId(externalId);
        if (eventOptional.isPresent()){
            var event = eventOptional.get();
            var konturEvent = new KonturEvent(event.getEventId(), event.getVersion() + 1);
            konturEvent.setObservationIds(event.getObservationIds());
            return konturEvent;
        }
        return new KonturEvent(UUID.randomUUID(), 1L);
    }
}
