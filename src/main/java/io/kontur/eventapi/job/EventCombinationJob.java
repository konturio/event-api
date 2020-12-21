package io.kontur.eventapi.job;

import io.kontur.eventapi.dao.KonturEventsDao;
import io.kontur.eventapi.dao.NormalizedObservationsDao;
import io.kontur.eventapi.entity.KonturEvent;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.eventcombination.EventCombinator;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class EventCombinationJob implements Runnable {
    public static final Logger LOG = LoggerFactory.getLogger(EventCombinationJob.class);

    private final NormalizedObservationsDao observationsDao;
    private final KonturEventsDao eventsDao;
    private final List<EventCombinator> eventCombinators;

    public EventCombinationJob(NormalizedObservationsDao observationsDao, KonturEventsDao eventsDao, List<EventCombinator> eventCombinators) {
        this.observationsDao = observationsDao;
        this.eventsDao = eventsDao;
        this.eventCombinators = eventCombinators;
    }

    @Override
    @Timed(value = "job.eventCombination", longTask = true)
    public void run() {
        List<NormalizedObservation> observations = observationsDao.getObservationsNotLinkedToEvent();

        //ideally order should not matter, but now firms processing is depending on order
        // (see https://kontur.fibery.io/Tasks/Task/Firms-observation-with-distance-less-then-1km-are-in-different-envents-4446)
        observations.sort(Comparator.comparing(NormalizedObservation::getSourceUpdatedAt));

        LOG.info("Combination job has started. Events to process: {}", observations.size());

        observations.forEach(this::addToEvent);

        LOG.info("Combination job has finished");
    }

    private void addToEvent(NormalizedObservation observation) {
        KonturEvent event = findEvent(observation).orElseGet(() -> new KonturEvent(UUID.randomUUID()));
        event.addObservations(observation.getObservationId());
        eventsDao.insertEvent(event);
    }

    private Optional<KonturEvent> findEvent(NormalizedObservation normalizedObservation) {
        EventCombinator eventCombinator = Applicable.get(eventCombinators, normalizedObservation);
        return eventCombinator.findEventForObservation(normalizedObservation);
    }
}
