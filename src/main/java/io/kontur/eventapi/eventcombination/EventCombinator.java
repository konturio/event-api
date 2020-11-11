package io.kontur.eventapi.eventcombination;

import io.kontur.eventapi.entity.KonturEvent;
import io.kontur.eventapi.entity.NormalizedObservation;

import java.util.Optional;

public interface EventCombinator {

    boolean isApplicable(NormalizedObservation normalizedObservation);

    Optional<KonturEvent> findEventForObservation(NormalizedObservation normalizedObservation);
}
