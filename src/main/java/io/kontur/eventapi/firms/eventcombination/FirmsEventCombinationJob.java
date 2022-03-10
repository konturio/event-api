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
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;

@Component
public class FirmsEventCombinationJob extends EventCombinationJob {

    private static final Logger LOG = LoggerFactory.getLogger(FirmsEventCombinationJob.class);

    public FirmsEventCombinationJob(NormalizedObservationsDao observationsDao, KonturEventsDao eventsDao,
                                    List<EventCombinator> eventCombinators, MeterRegistry meterRegistry) {
        super(observationsDao, eventsDao, eventCombinators, meterRegistry);
    }

    /**
     * 1. Obtain observations for 24 hours
     * 2. Search for existing events iteratively.
     * 3. Cluster remaining observations by geometry
     *    and create events from clusters.
     */
    @Override
    public void execute() {
        List<NormalizedObservation> observations = observationsDao
                .getFirmsObservationsNotLinkedToEventFor24Hours();

        if (!CollectionUtils.isEmpty(observations)) {
            LOG.info("Firms Combination processing: {} events", observations.size());

            Map<UUID, NormalizedObservation> observationByIds = observations.stream()
                    .collect(toMap(NormalizedObservation::getObservationId, identity()));
            findExistingEvents(observationByIds);
            addObservationsToNewEvents(observationByIds);
        }
    }

    /**
     * Try to find existing events for observations iteratively.
     * We need to check only if observations fit to changed events
     * after the first iteration.
     * Number of changed events will reduce with each iteration,
     * and on the last iteration it won't find any more existing
     * events fitting observations.
     *
     * @param observationByIds observations to match with existing events
     */
    private void findExistingEvents(Map<UUID, NormalizedObservation> observationByIds) {
        if (!observationByIds.isEmpty()) {
            Set<UUID> eventIds = tryFindEvents(observationByIds, null);
            while (!eventIds.isEmpty() && !observationByIds.isEmpty()) {
                eventIds = tryFindEvents(observationByIds, eventIds);
            }
        }
    }

    private Set<UUID> tryFindEvents(Map<UUID, NormalizedObservation> observationByIds, Set<UUID> eventIds) {
        List<KonturEvent> events = eventsDao.findClosestEventsToObservations(observationByIds.keySet(), eventIds);
        Set<UUID> changedEventIds = new HashSet<>();
        events.forEach(event -> {
            changedEventIds.add(event.getEventId());
            event.getObservationIds().forEach(observationId -> {
                NormalizedObservation observation = observationByIds.remove(observationId);
                addToEvent(event.getEventId(), observation);
            });
        });
        return changedEventIds;
    }

    /**
     * Cluster the remaining observations by geometry
     * and create new events from those clusters.
     *
     * @param observationByIds unmatched observations
     */
    private void addObservationsToNewEvents(Map<UUID, NormalizedObservation> observationByIds) {
        if (!observationByIds.isEmpty()) {
            List<Set<UUID>> clusters = observationsDao.clusterObservationsByGeography(observationByIds.keySet());
            clusters.forEach(cluster -> {
                UUID eventId = UUID.randomUUID();
                cluster.forEach(observationId -> addToEvent(eventId, observationByIds.get(observationId)));
            });
        }
    }

    private void addToEvent(UUID eventId, NormalizedObservation observation) {
        eventsDao.appendObservationIntoEvent(eventId, observation);
    }

    @Override
    public String getName() {
        return "firmsEventCombination";
    }
}
