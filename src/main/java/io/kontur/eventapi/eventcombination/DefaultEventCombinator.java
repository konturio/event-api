package io.kontur.eventapi.eventcombination;

import io.kontur.eventapi.dao.KonturEventsDao;
import io.kontur.eventapi.entity.KonturEvent;
import io.kontur.eventapi.entity.NormalizedObservation;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DefaultEventCombinator implements EventCombinator {
    private final KonturEventsDao eventsDao;

    public DefaultEventCombinator(KonturEventsDao eventsDao) {
        this.eventsDao = eventsDao;
    }

    @Override
    public boolean isDefault() {
        return true;
    }

    @Override
    public Optional<KonturEvent> findEventForObservation(NormalizedObservation observation) {
        return eventsDao.getEventByExternalId(observation.getExternalEventId());
    }
}
