package io.kontur.eventapi.jtwc.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.jtwc.client.JtwcClient;
import io.kontur.eventapi.jtwc.service.JtwcService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static io.kontur.eventapi.jtwc.job.JtwcImportJob.JTWC_PROVIDER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JtwcImportJobTest {

    @Mock
    private DataLakeDao dataLakeDao;

    @Mock
    private JtwcClient jtwcClient;

    @Captor
    private ArgumentCaptor<List<DataLake>> dataLakesCaptor;

    @AfterEach
    public void resetMocks() {
        reset(dataLakeDao);
        reset(jtwcClient);
    }

    @Test
    public void testImportJob() throws IOException {
        String feed = readResource("input_jtwc.xml");
        String text = readResource("ep0425web.txt");
        when(jtwcClient.getFeed()).thenReturn(feed);
        when(jtwcClient.getProduct("ep0425web.txt")).thenReturn(text);
        when(dataLakeDao.isNewEvent(anyString(), anyString(), anyString())).thenReturn(true);

        JtwcService service = new JtwcService(jtwcClient);
        JtwcImportJob job = new JtwcImportJob(new SimpleMeterRegistry(), service, dataLakeDao);

        job.run();

        verify(dataLakeDao, times(1)).storeDataLakes(dataLakesCaptor.capture());
        List<DataLake> stored = dataLakesCaptor.getValue();
        assertEquals(1, stored.size());
        DataLake dl = stored.get(0);
        assertEquals(JTWC_PROVIDER, dl.getProvider());
        assertNotNull(dl.getExternalId());
        assertNotNull(dl.getUpdatedAt());
        assertEquals(text, dl.getData());
    }

    private String readResource(String name) throws IOException {
        return IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream(name)), "UTF-8");
    }
}
