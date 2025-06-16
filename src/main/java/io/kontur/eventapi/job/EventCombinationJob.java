package io.kontur.eventapi.job;

import io.kontur.eventapi.dao.KonturEventsDao;
import io.kontur.eventapi.dao.NormalizedObservationsDao;
import io.kontur.eventapi.entity.KonturEvent;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.eventcombination.EventCombinator;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class EventCombinationJob extends AbstractJob  {
    public static final Logger LOG = LoggerFactory.getLogger(EventCombinationJob.class);

    protected final NormalizedObservationsDao observationsDao;
    protected final KonturEventsDao eventsDao;
    protected final List<EventCombinator> eventCombinators;

    @Value("${scheduler.eventCombination.providers}")
    private String[] sequentialProviders;

    public EventCombinationJob(NormalizedObservationsDao observationsDao, KonturEventsDao eventsDao, List<EventCombinator> eventCombinators, MeterRegistry meterRegistry) {
        super(meterRegistry);
        this.observationsDao = observationsDao;
        this.eventsDao = eventsDao;
        this.eventCombinators = eventCombinators;
    }

    @Override
    public void execute() {
        List<NormalizedObservation> observations = observationsDao
                .getObservationsNotLinkedToEvent(Arrays.asList(sequentialProviders));
        updateObservationsMetric(observations.size());

        if (!CollectionUtils.isEmpty(observations)) {
            LOG.info("Combination processing: {} events", observations.size());

            observations.forEach(this::addToEvent);
        }
    }

    @Override
    public String getName() {
        return "eventCombination";
    }

    @Timed(value = "eventCombination.observation.timer")
    @Counted(value = "eventCombination.observation.counter")
    private void addToEvent(NormalizedObservation observation) {
        KonturEvent event = findEvent(observation).orElseGet(() -> new KonturEvent(UUID.randomUUID()));
        event.addObservations(observation.getObservationId());
        eventsDao.appendObservationIntoEvent(event.getEventId(), observation);
    }

    protected Optional<KonturEvent> findEvent(NormalizedObservation normalizedObservation) {
        EventCombinator eventCombinator = Applicable.get(eventCombinators, normalizedObservation);
        return eventCombinator.findEventForObservation(normalizedObservation);
    }
}
