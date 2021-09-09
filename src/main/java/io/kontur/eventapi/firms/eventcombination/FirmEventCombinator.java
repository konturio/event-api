package io.kontur.eventapi.firms.eventcombination;

import io.kontur.eventapi.dao.KonturEventsDao;
import io.kontur.eventapi.entity.KonturEvent;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.eventcombination.EventCombinator;
import org.springframework.stereotype.Component;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.Geometry;

import java.util.Optional;

import static io.kontur.eventapi.firms.FirmsUtil.FIRMS_PROVIDERS;
import static io.kontur.eventapi.util.JsonUtil.writeJson;

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
    public Optional<KonturEvent> findEventForObservation(NormalizedObservation observation) {
        String geometry = getGeometry(observation);
        return eventsDao.getEventWithClosestObservation(observation.getSourceUpdatedAt(), geometry, FIRMS_PROVIDERS, null);
    }

    private String getGeometry(NormalizedObservation observation) {
        FeatureCollection featureCollection = observation.getGeometries();

        int featureCollectionLength = featureCollection.getFeatures().length;
        if (featureCollectionLength != 1) {
            throw new IllegalStateException("expected one geometry for firms feature collection, but found " + featureCollectionLength);
        }

        Geometry geometry = featureCollection.getFeatures()[0].getGeometry();
        return writeJson(geometry);
    }
}
