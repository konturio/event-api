package io.kontur.eventapi.job;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.dao.KonturEventsDao;
import io.kontur.eventapi.dao.NormalizedObservationsDao;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.Feed;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.episodecomposition.DefaultEpisodeCombinator;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class FeedCompositionJobErrorHandlingTest {

    private final KonturEventsDao eventsDao = mock(KonturEventsDao.class);
    private final FeedDao feedDao = mock(FeedDao.class);
    private final NormalizedObservationsDao observationsDao = mock(NormalizedObservationsDao.class);
    private final FeedCompositionJob job = new FeedCompositionJob(eventsDao, feedDao, observationsDao,
            List.of(new DefaultEpisodeCombinator()), new SimpleMeterRegistry());

    @Test
    public void skipEventOnJsonbSizeExceededAndContinueProcessingOthers() {
        UUID feedId = UUID.randomUUID();
        Feed feed = new Feed();
        feed.setFeedId(feedId);
        feed.setAlias("test-feed");
        feed.setEnrichment(Collections.emptyList());

        UUID event1 = UUID.randomUUID();
        UUID event2 = UUID.randomUUID();
        when(eventsDao.getEventsForRolloutEpisodes(feedId)).thenReturn(new LinkedHashSet<>(List.of(event1, event2)));
        when(feedDao.getLastFeedDataVersion(any(), any())).thenReturn(Optional.empty());

        NormalizedObservation observation = new NormalizedObservation();
        observation.setObservationId(UUID.randomUUID());
        observation.setProvider("provider1");
        OffsetDateTime now = OffsetDateTime.now();
        observation.setLoadedAt(now);
        observation.setStartedAt(now);
        observation.setEndedAt(now);
        observation.setSourceUpdatedAt(now);
        observation.setType(EventType.FLOOD);
        observation.setEventSeverity(Severity.MODERATE);

        when(observationsDao.getObservationsByEventId(any()))
                .thenAnswer(invocation -> new ArrayList<>(List.of(observation)));

        PSQLException psqlException = new PSQLException(
                "ERROR: total size of jsonb array elements exceeds the maximum of 268435455 bytes",
                PSQLState.STRING_DATA_RIGHT_TRUNCATION);

        doThrow(new DataAccessResourceFailureException("fail", psqlException))
                .when(feedDao).insertFeedData(argThat(fd -> fd.getEventId().equals(event1)), eq(feed.getAlias()));
        doNothing().when(feedDao).insertFeedData(argThat(fd -> fd.getEventId().equals(event2)), eq(feed.getAlias()));

        Logger logger = (Logger) LoggerFactory.getLogger(FeedCompositionJob.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        job.updateFeed(feed);

        logger.detachAppender(appender);

        verify(feedDao).insertFeedData(argThat(fd -> fd.getEventId().equals(event2)), eq(feed.getAlias()));

        boolean hasJsonbLog = appender.list.stream().anyMatch(event ->
                event.getFormattedMessage().contains("oversized jsonb") &&
                        event.getFormattedMessage().contains(event1.toString()) &&
                        event.getFormattedMessage().contains("provider1") &&
                        event.getFormattedMessage().contains(observation.getObservationId().toString())
        );
        assertTrue(hasJsonbLog, String.format(
                "Expected log message about oversized jsonb for event %s with observation %s",
                event1, observation.getObservationId()));
    }
}

