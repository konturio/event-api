package io.kontur.eventapi.job;

import io.kontur.eventapi.dao.KonturEventsDao;
import io.kontur.eventapi.dao.NormalizedObservationsDao;
import io.kontur.eventapi.entity.KonturEvent;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
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

        for (String externalId : externalIds) {
//            TODO all new event updates are combined into one new version at job start.
//             We have in a new version all updates since last job start.
//             E.g. job stars every minute - new event version created from observations that were created for the last minute.
//             job stars every 15 minute - new event version created from observations that were created for the 15 last minutes.
//             ----- Job needs to be rewritten so that no matter how often we run the job it closest observations together into a new version.
//             For instance observation were loaded at 10:15:41, 10:15:53, 10:16:10, 10:18:05, 10:22:30 ->
//             -> we would like to have 3 version, where: 1 version - 10:15:41, 10:15:53, 10:16:10; 2 version - v1 + 10:18:05; 3 version - v2 + 10:22:30

            processEvent(externalId);
        }

        LOG.info("Combination job has finished");
    }

    private void processEvent(String externalId) {
        var normalizedObservations = observationsDao.getObservationsByExternalId(externalId);
        var newEventVersion = createNewEventVersion(externalId);

        List<NormalizedObservation> filteredObservations = normalizedObservations.stream()
                .filter(obs -> newEventVersion.getObservationIds().stream()
                        .noneMatch(id -> obs.getObservationId().equals(id)))
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
        Optional<KonturEvent> event = eventsDao.getLatestEventByExternalId(externalId);
        return event.map(e -> new KonturEvent(e.getEventId(), e.getVersion() + 1, e.getObservationIds()))
                .orElseGet(() -> new KonturEvent(UUID.randomUUID(), 1L));
    }
}
