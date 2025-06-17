package io.kontur.eventapi.service;

import io.kontur.eventapi.client.KonturApiClient;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.util.JsonUtil;
import org.junit.jupiter.api.Test;
import org.wololo.geojson.FeatureCollection;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocationServiceTest {

    @Test
    void findGaulLocation() {
        KonturApiClient apiClient = mock(KonturApiClient.class);
        LocationService service = new LocationService(apiClient);

        NormalizedObservation observation = new NormalizedObservation();
        observation.setGeometries(JsonUtil.readJson("{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[10,10]},\"properties\":{}}]}", FeatureCollection.class));

        FeatureCollection boundaries = JsonUtil.readJson("{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"properties\":{\"admin_level\":\"0\",\"tags\":{\"name\":\"Country\"}}},{\"type\":\"Feature\",\"properties\":{\"admin_level\":\"1\",\"tags\":{\"name\":\"Region\"}}},{\"type\":\"Feature\",\"properties\":{\"admin_level\":\"2\",\"tags\":{\"name\":\"County\"}}}]}", FeatureCollection.class);
        when(apiClient.adminBoundaries(any())).thenReturn(boundaries);

        String location = service.findGaulLocation(Set.of(observation));
        assertEquals("Country, Region, County", location);
    }

    @Test
    void findGaulLocationReturnsNullWhenNoGeometry() {
        KonturApiClient apiClient = mock(KonturApiClient.class);
        LocationService service = new LocationService(apiClient);

        NormalizedObservation observation = new NormalizedObservation();
        when(apiClient.adminBoundaries(any())).thenReturn(null);

        String location = service.findGaulLocation(Collections.singleton(observation));
        assertNull(location);
    }
}
