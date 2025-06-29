package io.kontur.eventapi.jtwc.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.jtwc.JtwcUtil;
import io.kontur.eventapi.jtwc.parser.JtwcRssParser;
import io.kontur.eventapi.test.AbstractCleanableIntegrationTest;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.isA;

class JtwcImportJobIT extends AbstractCleanableIntegrationTest {

    private final DataLakeDao dataLakeDao;
    private final JtwcImportJob job;

    @MockBean
    private JtwcRssParser parser;

    @Autowired
    public JtwcImportJobIT(JdbcTemplate jdbcTemplate, DataLakeDao dataLakeDao,
                           JtwcImportJob job, FeedDao feedDao) {
        super(jdbcTemplate, feedDao);
        this.dataLakeDao = dataLakeDao;
        this.job = job;
    }

    @Test
    public void testJtwcImportJobRun() throws Exception {
        OffsetDateTime time = OffsetDateTime.now();
        Mockito.when(parser.parse()).thenReturn(List.of(new JtwcRssParser.RssItem("https://example.com/test.txt", time)));
        Mockito.when(parser.loadText(isA(String.class))).thenReturn("TEST DATA");
        job.run();
        List<DataLake> data = dataLakeDao.getDenormalizedEvents(List.of(JtwcUtil.JTWC_PROVIDER));
        assertEquals(1, data.size());
        DataLake dl = data.get(0);
        assertEquals(time, dl.getUpdatedAt());
        assertEquals(JtwcUtil.JTWC_PROVIDER, dl.getProvider());
        assertEquals(DigestUtils.md5Hex("TEST DATA"), dl.getExternalId());
        assertEquals("TEST DATA", dl.getData());
    }
}
