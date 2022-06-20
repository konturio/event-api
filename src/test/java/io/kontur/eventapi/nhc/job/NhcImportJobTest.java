package io.kontur.eventapi.nhc.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.nhc.client.NhcAtClient;
import io.kontur.eventapi.nhc.converter.NhcDataLakeConverter;
import io.kontur.eventapi.nhc.converter.NhcXmlParser;
import io.kontur.eventapi.nhc.service.NhcAtService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NhcImportJobTest {
    @Mock
    private DataLakeDao dataLakeDao;

    @Mock
    private NhcAtClient client;

    @Captor
    ArgumentCaptor<ArrayList<DataLake>> dataLakesCaptor;

    @AfterEach
    public void resetMocks() {
        reset(dataLakeDao);
        reset(client);
    }

    @Test
    public void testJobPositive() throws IOException {
        //given
        prepareMocks("input_nhc1.xml");
        NhcAtService service = new NhcAtService(dataLakeDao, new NhcDataLakeConverter(), client);
        NhcAtImportJob importJob = new NhcAtImportJob(service, new NhcXmlParser(), new SimpleMeterRegistry());

        //when
        importJob.run();

        //then
        verify(dataLakeDao, times(1)).storeDataLakes(dataLakesCaptor.capture());
        List<ArrayList<DataLake>> dataLakesInv = dataLakesCaptor.getAllValues();
        List<DataLake> dataLakes = dataLakesInv.get(0);
        assertNotNull(dataLakes);
        assertEquals(1, dataLakes.size());
    }

    @Test
    public void testNoItems() throws IOException {
        //given
        prepareMocks("input_nhc2.xml");
        NhcAtService service = new NhcAtService(dataLakeDao, new NhcDataLakeConverter(), client);
        NhcAtImportJob importJob = new NhcAtImportJob(service, new NhcXmlParser(), new SimpleMeterRegistry());

        //when
        importJob.run();

        //then
        verify(dataLakeDao, times(0)).storeDataLakes(dataLakesCaptor.capture());
        List<ArrayList<DataLake>> dataLakesInv = dataLakesCaptor.getAllValues();
        assertNotNull(dataLakesInv);
        assertEquals(0, dataLakesInv.size());
    }


    @Test
    public void testEmptyFeed() throws IOException {
        //given
        prepareMocks("input_nhc_empty_feed.xml");
        NhcAtService service = new NhcAtService(dataLakeDao, new NhcDataLakeConverter(), client);
        NhcAtImportJob importJob = new NhcAtImportJob(service, new NhcXmlParser(), new SimpleMeterRegistry());

        //when
        importJob.run();

        //then
        verify(dataLakeDao, times(0)).storeDataLakes(dataLakesCaptor.capture());
        List<ArrayList<DataLake>> dataLakesInv = dataLakesCaptor.getAllValues();
        assertNotNull(dataLakesInv);
        assertEquals(0, dataLakesInv.size());
    }

    private String readMessageFromFile(String fileName) throws IOException {
        return IOUtils.toString(Objects.requireNonNull(this.getClass().getResourceAsStream(fileName)), "UTF-8");
    }

    private void prepareMocks(String fileName) throws IOException {
        String inputXml = readMessageFromFile(fileName);
        when(client.getXml()).thenReturn(inputXml);
        when(dataLakeDao.isNewEvent(isA(String.class), isA(String.class), isA(String.class))).thenReturn(true);
        doNothing().when(dataLakeDao).storeDataLakes(anyList());
    }

}
