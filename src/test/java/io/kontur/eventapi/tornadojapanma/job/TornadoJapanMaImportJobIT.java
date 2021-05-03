package io.kontur.eventapi.tornadojapanma.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.test.AbstractCleanableIntegrationTest;
import io.kontur.eventapi.tornadojapanma.dto.MainDamageSituation;
import io.kontur.eventapi.tornadojapanma.dto.ParsedCase;
import io.kontur.eventapi.tornadojapanma.parser.TornadoJapanMaHtmlParser;
import io.kontur.eventapi.tornadojapanma.service.TornadoJapanMaImportService;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static io.kontur.eventapi.tornadojapanma.service.TornadoJapanMaImportService.TORNADO_JAPAN_MA_PROVIDER;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.isA;

class TornadoJapanMaImportJobIT extends AbstractCleanableIntegrationTest {

    private final DataLakeDao dataLakeDao;
    private final TornadoJapanMaImportJob job;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OffsetDateTime testUpdatedAt = OffsetDateTime.now();

    @MockBean
    private TornadoJapanMaHtmlParser parser;

    @MockBean
    private TornadoJapanMaImportService service;

    @Autowired
    public TornadoJapanMaImportJobIT(JdbcTemplate jdbcTemplate, DataLakeDao dataLakeDao,
                                     TornadoJapanMaImportJob job) {
        super(jdbcTemplate);
        this.dataLakeDao = dataLakeDao;
        this.job = job;
    }

    @Test
    public void testTornadoJapanMaImportJobRun() throws IOException {
        ParsedCase testCase = getTestData();
        String json = objectMapper.writeValueAsString(testCase);
        String hash = DigestUtils.md5Hex(json);

        Mockito.when(parser.parseUpdatedAt()).thenReturn("");
        Mockito.when(service.convertDate(isA(String.class))).thenReturn(testUpdatedAt);
        Mockito.when(parser.parseCases()).thenReturn(Set.of(testCase));
        job.run();

        List<DataLake> dataLakes = dataLakeDao.getDenormalizedEvents();
        assertEquals(1, dataLakes.size());

        DataLake dataLake = dataLakes.get(0);
        assertNotNull(dataLake.getObservationId());
        assertNotNull(dataLake.getLoadedAt());
        assertEquals(testUpdatedAt, dataLake.getUpdatedAt());
        assertEquals(TORNADO_JAPAN_MA_PROVIDER, dataLake.getProvider());
        assertEquals(hash, dataLake.getExternalId());
        assertEquals(json, dataLake.getData());
    }

    private ParsedCase getTestData() {
        return ParsedCase.builder()
                .type("竜巻または漏斗雲")
                .occurrenceDateTime("2012/08/30 12:50頃")
                .occurrencePlace("沖縄県 (海上)")
                .fScale("JEF0")
                .damageWidth("40")
                .damageLength("0.07")
                .mainDamageSituation(new MainDamageSituation("0", "0", "0", "0"))
                .viewingArea("暖気の移流")
                .remarks("")
                .build();
    }
}