package io.kontur.eventapi.eventcombination;

import io.kontur.eventapi.entity.KonturEvent;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.job.Applicable;

import java.util.Optional;

public interface EventCombinator extends Applicable<NormalizedObservation> {

    Optional<KonturEvent> findEventForObservation(NormalizedObservation observation);
}
