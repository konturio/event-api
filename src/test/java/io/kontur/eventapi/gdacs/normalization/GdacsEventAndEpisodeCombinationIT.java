package io.kontur.eventapi.gdacs.normalization;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.dao.mapper.FeedMapper;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.SortOrder;
import io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter;
import io.kontur.eventapi.gdacs.dto.ParsedAlert;
import io.kontur.eventapi.job.EventCombinationJob;
import io.kontur.eventapi.job.FeedCompositionJob;
import io.kontur.eventapi.job.NormalizationJob;
import io.kontur.eventapi.test.AbstractCleanableIntegrationTest;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

//todo: add more tests
public class GdacsEventAndEpisodeCombinationIT extends AbstractCleanableIntegrationTest {

    private final GdacsDataLakeConverter gdacsDataLakeConverter;
    private final DataLakeDao dataLakeDao;

    private final NormalizationJob normalizationJob;
    private final EventCombinationJob eventCombinationJob;
    private final FeedCompositionJob feedCompositionJob;
    private final FeedMapper feedMapper;


    private final static String externalId = "GDACS_EQ_1239039_9997370";

    @Autowired
    public GdacsEventAndEpisodeCombinationIT(GdacsDataLakeConverter gdacsDataLakeConverter, DataLakeDao dataLakeDao,
                                             JdbcTemplate jdbcTemplate, NormalizationJob normalizationJob,
                                             EventCombinationJob eventCombinationJob, FeedCompositionJob feedCompositionJob,
                                             FeedMapper feedMapper) {
        super(jdbcTemplate);
        this.gdacsDataLakeConverter = gdacsDataLakeConverter;
        this.dataLakeDao = dataLakeDao;
        this.normalizationJob = normalizationJob;
        this.eventCombinationJob = eventCombinationJob;
        this.feedCompositionJob = feedCompositionJob;
        this.feedMapper = feedMapper;
    }

    @Test
    public void testFeedComposition() throws IOException {
        //given
        var dataLakes = getDataLakeList();
        dataLakeDao.storeEventData(dataLakes.get(0));
        dataLakeDao.storeEventData(dataLakes.get(1));

        //when
        normalizationJob.run();
        eventCombinationJob.run();
        feedCompositionJob.run();

        //then
        List<FeedData> feedData = searchFeedData();
        assertEquals(1, feedData.size());
    }


    private List<FeedData> searchFeedData() {
        List<FeedData> firms = feedMapper.searchForEvents(
                "gdacs",
                List.of(EventType.EARTHQUAKE),
                null,
                null,
                OffsetDateTime.parse("2000-11-02T11:00Z"),
                100,
                List.of(),
                SortOrder.ASC,
                null,
                null,
                null,
                null
        );
        firms.sort(Comparator.comparing(f -> f.getObservations().size()));
        return firms;
    }


    private List<DataLake> getDataLakeList() throws IOException {
        var parsedAlert = new ParsedAlert();
        parsedAlert.setIdentifier(externalId);
        parsedAlert.setDateModified(OffsetDateTime.of(LocalDateTime.of(2020, 10, 12, 9, 33, 22), ZoneOffset.UTC));
        parsedAlert.setSent(OffsetDateTime.parse("2020-10-12T05:03:07-00:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        parsedAlert.setData(readMessageFromFile("alert.xml"));

        return List.of(
                gdacsDataLakeConverter.convertGdacs(parsedAlert),
                gdacsDataLakeConverter.convertGdacsWithGeometry(parsedAlert, readMessageFromFile("geometry.json"))
        );
    }

    private String readMessageFromFile(String fileName) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(fileName), "UTF-8");
    }
}