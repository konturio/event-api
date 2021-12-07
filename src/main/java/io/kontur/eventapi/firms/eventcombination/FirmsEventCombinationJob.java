package io.kontur.eventapi.firms.eventcombination;

import io.kontur.eventapi.dao.KonturEventsDao;
import io.kontur.eventapi.dao.NormalizedObservationsDao;
import io.kontur.eventapi.entity.KonturEvent;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.eventcombination.EventCombinator;
import io.kontur.eventapi.job.EventCombinationJob;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;

import static io.kontur.eventapi.firms.FirmsUtil.FIRMS_PROVIDERS;
import static io.kontur.eventapi.util.JsonUtil.writeJson;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

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
     * 1. Obtain observations for 24 hours
     * 2. Search for existing events iteratively.
     * 3. Cluster remaining observations by geometry
     *    and create events.
     */
    @Override
    public void execute() {
        List<NormalizedObservation> observations = observationsDao
                .getObservationsNotLinkedToEventFor24Hours(Arrays.asList(sequentialProviders));

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
    @Timed(value = "firmsEventCombination.findExistingEvents.timer")
    @Counted(value = "firmsEventCombination.findExistingEvents.counter")
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
    @Timed(value = "firmsEventCombination.addObservationsToExistingEvents.timer")
    @Counted(value = "firmsEventCombination.addObservationsToExistingEvents.counter")
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
     * Cluster observations by geometry to get
     * new events.
     *
     * @param observations unmatched observations
     */
    @Timed(value = "firmsEventCombination.addObservationsToNewEvents.timer")
    @Counted(value = "firmsEventCombination.addObservationsToNewEvents.counter")
    private void addObservationsToNewEvents(List<NormalizedObservation> observations) {
        if (observations.isEmpty()) {
            return;
        }
        Map<UUID, NormalizedObservation> observationsByIds = observations.stream()
                .collect(toMap(NormalizedObservation::getObservationId, Function.identity()));
        List<Set<UUID>> clusters = observationsDao.clusterObservationsByGeography(observationsByIds.keySet());
        clusters.forEach(clusterObservationIds -> createEvent(
                clusterObservationIds.stream().map(observationsByIds::get).collect(toList())));
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

    private void createEvent(List<NormalizedObservation> observations) {
        KonturEvent event = new KonturEvent(UUID.randomUUID());
        observations.forEach(observation -> addToEvent(observation, event));
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
