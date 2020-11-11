package io.kontur.eventapi.firms.eventcombination;

import io.kontur.eventapi.dao.KonturEventsDao;
import io.kontur.eventapi.entity.KonturEvent;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.eventcombination.EventCombinator;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static io.kontur.eventapi.firms.FirmsUtil.FIRMS_PROVIDERS;

@Component
public class FirmEventCombinator implements EventCombinator {
    private final KonturEventsDao eventsDao;

    public FirmEventCombinator(KonturEventsDao eventsDao) {
        this.eventsDao = eventsDao;
    }

    @Override
    public boolean isApplicable(NormalizedObservation normalizedObservation) {
        return FIRMS_PROVIDERS.contains(normalizedObservation.getProvider());
    }

    @Override
    public Optional<KonturEvent> findEventForObservation(NormalizedObservation normalizedObservation) {
        return eventsDao.getEventWithClosestObservation(normalizedObservation.getStartedAt(), normalizedObservation.getGeometries());
    }
}
