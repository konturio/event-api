package io.kontur.eventapi.usgs.earthquake.event;

import io.kontur.eventapi.dao.KonturEventsDao;
import io.kontur.eventapi.dao.NormalizedObservationsDao;
import io.kontur.eventapi.entity.KonturEvent;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.eventcombination.DefaultEventCombinator;
import io.kontur.eventapi.job.EventCombinationJob;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class UsgsEarthquakeEventCombinationJobTest {

    @Test
    void mergeSameEarthquakeUpdates() {
        NormalizedObservationsDao observationsDao = Mockito.mock(NormalizedObservationsDao.class);
        KonturEventsDao eventsDao = Mockito.mock(KonturEventsDao.class);

        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        NormalizedObservation first = new NormalizedObservation();
        first.setObservationId(firstId);
        first.setExternalEventId("eq1");
        first.setProvider("usgs.earthquake");
        NormalizedObservation second = new NormalizedObservation();
        second.setObservationId(secondId);
        second.setExternalEventId("eq1");
        second.setProvider("usgs.earthquake");

        Mockito.when(observationsDao.getObservationsNotLinkedToEvent(Mockito.anyList()))
                .thenReturn(List.of(first, second));

        Map<String, KonturEvent> events = new HashMap<>();
        Mockito.when(eventsDao.getEventByExternalId(Mockito.anyString()))
                .thenAnswer(inv -> Optional.ofNullable(events.get(inv.getArgument(0))));
        Mockito.doAnswer(inv -> {
            UUID eventId = inv.getArgument(0);
            NormalizedObservation obs = inv.getArgument(1);
            events.computeIfAbsent(obs.getExternalEventId(), id -> new KonturEvent(eventId))
                  .addObservations(obs.getObservationId());
            return null;
        }).when(eventsDao).appendObservationIntoEvent(Mockito.any(UUID.class), Mockito.any(NormalizedObservation.class));

        EventCombinationJob job = new EventCombinationJob(
                observationsDao,
                eventsDao,
                List.of(new DefaultEventCombinator(eventsDao)),
                new SimpleMeterRegistry());

        ReflectionTestUtils.setField(job, "sequentialProviders", new String[]{"usgs.earthquake"});

        job.run();

        assertEquals(2, events.get("eq1").getObservationIds().size(),
                "Merged earthquake event should contain two observations sharing external ID 'eq1'");
    }
}
