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

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class EventCombinationJob2 implements Runnable {
    public static final Logger LOG = LoggerFactory.getLogger(EventCombinationJob2.class);

    private final NormalizedObservationsDao observationsDao;
    private final KonturEventsDao eventsDao;
    private final List<EventCombinator> eventCombinators;

    public EventCombinationJob2(NormalizedObservationsDao observationsDao, KonturEventsDao eventsDao, List<EventCombinator> eventCombinators) {
        this.observationsDao = observationsDao;
        this.eventsDao = eventsDao;
        this.eventCombinators = eventCombinators;
    }

    @Override
    @Timed("job.eventCombination2")
    public void run() {
        List<NormalizedObservation> observations = observationsDao.getObservationsNotLinkedToEvent();

        LOG.info("Combination job has started. Events to process: {}", observations.size());

        observations.forEach(this::addToEvent);

        LOG.info("Combination job has finished");
    }

    private void addToEvent(NormalizedObservation observation) {
        KonturEvent event = findEvent(observation).orElseGet(() -> new KonturEvent(UUID.randomUUID(), 1L));
        event.addObservations(observation.getObservationId());
        eventsDao.insertEventVersion(event);
    }

    private Optional<KonturEvent> findEvent(NormalizedObservation normalizedObservation) {
        EventCombinator eventCombinator = findEventCombinator(normalizedObservation);
        return eventCombinator.findEventForObservation(normalizedObservation);
    }

    private EventCombinator findEventCombinator(NormalizedObservation observation) {
        List<EventCombinator> eventCombinatorsForProvider = eventCombinators.stream()
                .filter(combinator -> combinator.isApplicable(observation))
                .collect(Collectors.toList());

        if (eventCombinatorsForProvider.size() > 1) {
            throw new IllegalStateException("found more then 1 event combinator for provider: " + observation.getProvider());
        }

        if (eventCombinatorsForProvider.isEmpty()) {
            throw new IllegalStateException("combinator not found for provider: " + observation.getProvider());
        }

        return eventCombinatorsForProvider.get(0);
    }
}
