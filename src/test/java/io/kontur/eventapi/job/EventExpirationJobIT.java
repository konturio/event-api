package io.kontur.eventapi.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.dao.KonturEventsDao;
import io.kontur.eventapi.dao.NormalizedObservationsDao;
import io.kontur.eventapi.entity.*;
import io.kontur.eventapi.test.AbstractCleanableIntegrationTest;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.io.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static io.kontur.eventapi.TestUtil.readFile;
import static io.kontur.eventapi.entity.EventType.FLOOD;
import static io.kontur.eventapi.entity.Severity.EXTREME;
import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.PDC_SQS_PROVIDER;
import static io.kontur.eventapi.pdc.normalization.PdcHazardNormalizer.ORIGIN_NASA;
import static io.kontur.eventapi.util.DateTimeUtil.getDateTimeFromMilli;
import static io.kontur.eventapi.util.GeometryUtil.AREA_TYPE_PROPERTY;
import static io.kontur.eventapi.util.GeometryUtil.CENTER_POINT;
import static io.kontur.eventapi.util.LossUtil.INFRASTRUCTURE_REPLACEMENT_VALUE;
import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.*;

public class EventExpirationJobIT extends AbstractCleanableIntegrationTest {

	private final NormalizationJob normalizationJob;
	private final EventCombinationJob eventCombinationJob;
	private final FeedCompositionJob feedCompositionJob;
	private final EventExpirationJob eventExpirationJob;
	private final DataLakeDao dataLakeDao;
	private final NormalizedObservationsDao normalizedObservationsDao;
	private final KonturEventsDao konturEventsDao;
	private final FeedDao feedDao;

	private final OffsetDateTime pastDate = getDateTimeFromMilli(1671132808000L);
	private final OffsetDateTime futureDate = getDateTimeFromMilli(2023435120000L);
	private final String name = "Flood - SW of Mendi, Southern Highlands, Papua New Guinea";
	private final String region = "SW of Mendi, Southern Highlands, Papua New Guinea";
	private final String description = "The NASA Global Flood Model has issued a Flood Warning. It is estimated that 31,526 people, 5,960 households, and $37.73 Million of infrastructure* are within the affected area(s).";
	private final String externalEventId = "c0665db3-836f-459d-a0fa-f89ab3afba2b";
	private final List<String> urls = List.of("https://hazardbrief.pdc.org/PRODUCTION/ui/index.html?uuid=c0665db3-836f-459d-a0fa-f89ab3afba2b");
	private final Object loss = 37730000;


	@Autowired
	public EventExpirationJobIT(JdbcTemplate jdbcTemplate, FeedDao feedDao, NormalizationJob normalizationJob,
	                            EventCombinationJob eventCombinationJob, FeedCompositionJob feedCompositionJob,
	                            EventExpirationJob eventExpirationJob, DataLakeDao dataLakeDao, NormalizedObservationsDao normalizedObservationsDao, KonturEventsDao konturEventsDao) throws ParseException {
		super(jdbcTemplate, feedDao);
		this.normalizationJob = normalizationJob;
		this.eventCombinationJob = eventCombinationJob;
		this.feedCompositionJob = feedCompositionJob;
		this.eventExpirationJob = eventExpirationJob;
		this.dataLakeDao = dataLakeDao;
		this.normalizedObservationsDao = normalizedObservationsDao;
		this.konturEventsDao = konturEventsDao;
		this.feedDao = feedDao;
	}

	@Test
	public void testActiveEvent() throws IOException {
		runTest("activeEvent.json", true, false, true, true);
	}

	@Test
	public void testExpiredEvent() throws IOException {
		runTest("expiredEvent.json", true, false, false, false);
	}

	@Test
	public void testAutoExpireActiveEvent() throws IOException {
		runTest("autoExpireActiveEvent.json", false, true, true, true);
	}

	@Test
	public void testAutoExpireExpiredEvent() throws IOException {
		runTest("autoExpireExpiredEvent.json", true, true, true, false);
	}

	private void runTest(String fileName, boolean pastEvent, boolean autoExpire, boolean active, boolean expectedActive) throws IOException {
		DataLake dataLake = createDataLake(fileName);
		dataLakeDao.storeEventData(dataLake);

                normalizationJob.run(List.of(PDC_SQS_PROVIDER));
		List<NormalizedObservation> observations = normalizedObservationsDao.getObservationsNotLinkedToEvent(List.of(PDC_SQS_PROVIDER));
		assertEquals(1, observations.size());
		NormalizedObservation observation = observations.get(0);
		checkObservation(dataLake, observation, pastEvent, autoExpire, active);

		eventCombinationJob.run();
		KonturEvent konturEvent = konturEventsDao.getEventByExternalId(externalEventId).get();
		checkKonturEvent(konturEvent, dataLake);

		feedCompositionJob.run();
		Feed feed = feedDao.getFeedsByAliases(List.of("test-feed")).get(0);
		FeedData event = feedDao.getFeedDataByFeedIdAndEventId(feed.getFeedId(), konturEvent.getEventId()).get();
		checkFeedData(event, dataLake, pastEvent, autoExpire, active);

		eventExpirationJob.run();
		FeedData expiredEvent = feedDao.getFeedDataByFeedIdAndEventId(feed.getFeedId(), konturEvent.getEventId()).get();
		assertEquals(expectedActive, expiredEvent.getActive());
	}

	private void checkObservation(DataLake dataLake, NormalizedObservation observation, Boolean pastEvent, Boolean autoExpire, Boolean active) {
		assertEquals(dataLake.getObservationId(), observation.getObservationId());
		assertEquals(PDC_SQS_PROVIDER, observation.getProvider());
		assertEquals(externalEventId, observation.getExternalEventId());
		assertEquals(ORIGIN_NASA, observation.getOrigin());
		assertEquals(name, observation.getName());
		assertNull(observation.getProperName());
		assertEquals(description, observation.getDescription());
		assertEquals(description, observation.getEpisodeDescription());
		assertEquals(FLOOD, observation.getType());
		assertEquals(EXTREME, observation.getEventSeverity());
		assertEquals(active, observation.getActive());
		assertEquals(pastEvent ? pastDate : futureDate, observation.getStartedAt());
		assertEquals(pastEvent ? pastDate : futureDate, observation.getEndedAt());
		assertEquals(pastEvent ? pastDate : futureDate, observation.getSourceUpdatedAt());
		assertEquals(region, observation.getRegion());
		assertEquals(urls, observation.getUrls());
		assertNull(observation.getCost());
		assertEquals(loss, observation.getLoss().get(INFRASTRUCTURE_REPLACEMENT_VALUE));
		assertEquals(autoExpire, observation.getAutoExpire());
		assertEquals(1, observation.getGeometries().getFeatures().length);
		assertEquals(CENTER_POINT, observation.getGeometries().getFeatures()[0].getProperties().get(AREA_TYPE_PROPERTY));
	}

	private void checkKonturEvent(KonturEvent konturEvent, DataLake dataLake) {
		assertEquals(1, konturEvent.getObservationIds().size());
		assertEquals(konturEvent.getObservationIds().toArray()[0], dataLake.getObservationId());
	}

	private void checkFeedData(FeedData event, DataLake dataLake, Boolean pastEvent, Boolean autoExpire, Boolean active) {
		assertEquals(name, event.getName());
		assertNull(event.getProperName());
		assertEquals(description, event.getDescription());
		assertEquals(FLOOD, event.getType());
		assertEquals(EXTREME, event.getSeverity());
		assertEquals(active, event.getActive());
		assertEquals(pastEvent ? pastDate : futureDate, event.getStartedAt());
		assertEquals(pastEvent ? pastDate : futureDate, event.getEndedAt());
		assertEquals(region, event.getLocation());
		assertEquals(urls, event.getUrls());
		assertEquals(Set.of(dataLake.getObservationId()), event.getObservations());
		assertEquals(loss, event.getLoss().get(INFRASTRUCTURE_REPLACEMENT_VALUE));
		assertEquals(autoExpire, event.getAutoExpire());
		assertEquals(1, event.getGeometries().getFeatures().length);
		assertEquals(CENTER_POINT, event.getGeometries().getFeatures()[0].getProperties().get(AREA_TYPE_PROPERTY));
		assertEquals(1, event.getEpisodes().size());

		FeedEpisode episode = event.getEpisodes().get(0);
		assertEquals(name, episode.getName());
		assertNull(episode.getProperName());
		assertEquals(description, episode.getDescription());
		assertEquals(FLOOD, episode.getType());
		assertEquals(EXTREME, episode.getSeverity());
		assertEquals(pastEvent ? pastDate : futureDate, episode.getStartedAt());
		assertEquals(pastEvent ? pastDate : futureDate, episode.getEndedAt());
		assertEquals(pastEvent ? pastDate : futureDate, episode.getSourceUpdatedAt());
		assertEquals(region, episode.getLocation());
		assertEquals(urls, episode.getUrls());
		assertEquals(Set.of(dataLake.getObservationId()), episode.getObservations());
		assertEquals(loss, episode.getLoss().get(INFRASTRUCTURE_REPLACEMENT_VALUE));
		assertEquals(1, episode.getGeometries().getFeatures().length);
		assertEquals(CENTER_POINT, episode.getGeometries().getFeatures()[0].getProperties().get(AREA_TYPE_PROPERTY));
	}

	private DataLake createDataLake(String fileName) throws IOException {
		String data = readFile(this, fileName);
		DataLake dataLake = new DataLake();
		dataLake.setObservationId(UUID.randomUUID());
		dataLake.setExternalId("fafa2324-2f74-55ca-9414-7f31afdaa998");
		dataLake.setProvider(PDC_SQS_PROVIDER);
		dataLake.setLoadedAt(OffsetDateTime.now().atZoneSameInstant(UTC).toOffsetDateTime());
		dataLake.setData(data);
		return dataLake;
	}

}
