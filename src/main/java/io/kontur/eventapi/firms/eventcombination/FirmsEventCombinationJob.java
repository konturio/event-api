package io.kontur.eventapi.firms.eventcombination;

import io.kontur.eventapi.dao.KonturEventsDao;
import io.kontur.eventapi.dao.NormalizedObservationsDao;
import io.kontur.eventapi.entity.KonturEvent;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.eventcombination.EventCombinator;
import io.kontur.eventapi.job.EventCombinationJob;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

import static io.kontur.eventapi.firms.FirmsUtil.FIRMS_PROVIDERS;
import static io.kontur.eventapi.util.JsonUtil.writeJson;

@Component
public class FirmsEventCombinationJob extends EventCombinationJob {

    private static final Logger LOG = LoggerFactory.getLogger(FirmsEventCombinationJob.class);

    @Value("${scheduler.eventCombination.firmsProviders}")
    private String[] sequentialProviders;

    public FirmsEventCombinationJob(NormalizedObservationsDao observationsDao, KonturEventsDao eventsDao,
                                    List<EventCombinator> eventCombinators, MeterRegistry meterRegistry) {
        super(observationsDao, eventsDao, eventCombinators, meterRegistry);
    }

    /**
     * 1. Search for existing events iteratively.
     * 2. Create new events and add remaining observations
     *    to them iteratively
     */
    @Override
    public void execute() {
        List<NormalizedObservation> observations = observationsDao
                .getObservationsNotLinkedToEventOrderByGeography(Arrays.asList(sequentialProviders));

        LOG.info("Firms Combination processing: {} events", observations.size());

        Set<UUID> events = findExistingEvents(observations);
        addObservationsToExistingEvents(events, observations);
        addObservationsToNewEvents(observations);
    }

    /**
     * Try to find existing event for each observation
     * and save all the found events.
     * We don't need to check other events, cause remaining
     * observations can be added only to changed events
     * after first iteration.
     *
     * @param observations list of observations to search events for
     * @return set of event IDs to which observations were found
     */
    private Set<UUID> findExistingEvents(List<NormalizedObservation> observations) {
        Set<UUID> events = new HashSet<>();
        observations.removeIf(observation -> {
            Optional<UUID> eventIdOpt = tryFindEvent(observation, null);
            if (eventIdOpt.isPresent()) {
                events.add(eventIdOpt.get());
                return true;
            }
            return false;
        });
        return events;
    }

    /**
     * Try to add remaining observations only to changed events.
     * Save events that were changed on each iteration, their
     * count will reduce in iterations. And it'll come to situation
     * when no more observations can be added to existing events.
     *
     * @param events events that were changed in first iteration
     *               (observations were added to them)
     * @param observations unmatched observations
     */
    private void addObservationsToExistingEvents(Set<UUID> events, List<NormalizedObservation> observations) {
        while (!events.isEmpty()) {
            Set<UUID> changedEvents = new HashSet<>();
            observations.removeIf(observation -> {
                Optional<UUID> eventIdOpt = tryFindEvent(observation, events);
                if (eventIdOpt.isPresent()) {
                    changedEvents.add(eventIdOpt.get());
                    return true;
                }
                return false;
            });
            events.retainAll(changedEvents);
        }
    }

    /**
     * Observations are sorted by geography.
     * Store all the created events and try to link
     * new observation to all of the created events.
     *
     * We only make one iteration by observations here
     *
     * @param observations unmatched observations
     */
    private void addObservationsToNewEvents(List<NormalizedObservation> observations) {
        if (!observations.isEmpty()) {
            Set<UUID> createdEvents = new HashSet<>();
            createdEvents.add(createEvent(observations.get(0)));
            for (int i = 1; i < observations.size(); i++) {
                NormalizedObservation observation = observations.get(i);
                if (tryFindEvent(observation, createdEvents).isEmpty()) {
                    createdEvents.add(createEvent(observation));
                }
            }
        }
    }

    private Optional<UUID> tryFindEvent(NormalizedObservation observation, Set<UUID> eventIds) {
        String geometry = writeJson(observation.getGeometries().getFeatures()[0].getGeometry());
        Optional<KonturEvent> eventOpt = eventsDao.getEventWithClosestObservation(observation.getSourceUpdatedAt(), geometry, FIRMS_PROVIDERS, eventIds);
        if (eventOpt.isPresent()) {
            KonturEvent event = eventOpt.get();
            addToEvent(observation, event);
            return Optional.of(event.getEventId());
        }
        return Optional.empty();
    }

    private UUID createEvent(NormalizedObservation observation) {
        UUID eventId = UUID.randomUUID();
        addToEvent(observation, new KonturEvent(eventId));
        return eventId;
    }

    private void addToEvent(NormalizedObservation observation, KonturEvent event) {
        event.addObservations(observation.getObservationId());
        eventsDao.appendObservationIntoEvent(event, observation);
    }

    @Override
    public String getName() {
        return "firmsEventCombination";
    }
}
