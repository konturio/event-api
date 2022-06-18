package io.kontur.eventapi.inciweb.job;

import static io.kontur.eventapi.inciweb.job.InciWebImportJob.INCIWEB_PROVIDER;
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
import io.kontur.eventapi.inciweb.client.InciWebClient;
import io.kontur.eventapi.inciweb.converter.InciWebDataLakeConverter;
import io.kontur.eventapi.inciweb.converter.InciWebXmlParser;
import io.kontur.eventapi.inciweb.service.InciWebService;
import io.kontur.eventapi.util.DateTimeUtil;
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
class InciWebImportJobTest {
    @Mock
    private DataLakeDao dataLakeDao;

    @Mock
    private InciWebClient inciWebClient;

    @Captor
    ArgumentCaptor<ArrayList<DataLake>> dataLakesCaptor;

    @AfterEach
    public void resetMocks() {
        reset(dataLakeDao);
        reset(inciWebClient);
    }

    @Test
    public void testJobPositive() throws IOException {
        //given
        prepareMocks("input3.xml");
        InciWebService inciWebService = new InciWebService(dataLakeDao, new InciWebDataLakeConverter(), inciWebClient);
        InciWebImportJob inciWebImportJob = new InciWebImportJob(inciWebService, new InciWebXmlParser(), new SimpleMeterRegistry());

        //when
        inciWebImportJob.run();

        //then
        verify(dataLakeDao, times(1)).storeDataLakes(dataLakesCaptor.capture());
        List<ArrayList<DataLake>> dataLakesInv = dataLakesCaptor.getAllValues();
        List<DataLake> dataLakes = dataLakesInv.get(0);
        assertNotNull(dataLakes);
        assertEquals(3, dataLakes.size());
    }

    @Test
    public void testJobWithOneWrongItem() throws IOException {
        //given
        prepareMocks("input2.xml");
        InciWebService inciWebService = new InciWebService(dataLakeDao, new InciWebDataLakeConverter(), inciWebClient);
        InciWebImportJob inciWebImportJob = new InciWebImportJob(inciWebService, new InciWebXmlParser(), new SimpleMeterRegistry());

        //when
        inciWebImportJob.run();

        //then
        verify(dataLakeDao, times(1)).storeDataLakes(dataLakesCaptor.capture());
        List<ArrayList<DataLake>> dataLakesInv = dataLakesCaptor.getAllValues();
        List<DataLake> dataLakes = dataLakesInv.get(0);
        assertNotNull(dataLakes);
        assertEquals(2, dataLakes.size());
    }

    @Test
    public void testOneItem() throws IOException {
        //given
        prepareMocks("input1.xml");
        InciWebService inciWebService = new InciWebService(dataLakeDao, new InciWebDataLakeConverter(), inciWebClient);
        InciWebImportJob inciWebImportJob = new InciWebImportJob(inciWebService, new InciWebXmlParser(), new SimpleMeterRegistry());

        //when
        inciWebImportJob.run();
        verify(dataLakeDao, times(1)).storeDataLakes(dataLakesCaptor.capture());
        List<ArrayList<DataLake>> dataLakesInv = dataLakesCaptor.getAllValues();
        List<DataLake> dataLakes = dataLakesInv.get(0);

        //then
        assertNotNull(dataLakes);
        assertEquals(1, dataLakes.size());
        DataLake dataLake = dataLakes.get(0);
        assertNotNull(dataLake);
        assertEquals("http://example.com/incident/1/", dataLake.getExternalId());
        assertEquals(INCIWEB_PROVIDER, dataLake.getProvider());
        assertEquals(DateTimeUtil.parseDateTimeFromString("Fri, 03 Dec 2021 01:00:00 -06:00"), dataLake.getUpdatedAt());
    }

    private String readMessageFromFile(String fileName) throws IOException {
        return IOUtils.toString(Objects.requireNonNull(this.getClass().getResourceAsStream(fileName)), "UTF-8");
    }

    private void prepareMocks(String fileName) throws IOException {
        String inputXml = readMessageFromFile(fileName);
        when(inciWebClient.getXml()).thenReturn(inputXml);
        when(dataLakeDao.isNewEvent(isA(String.class), isA(String.class), isA(String.class))).thenReturn(true);
        doNothing().when(dataLakeDao).storeDataLakes(anyList());
    }
}