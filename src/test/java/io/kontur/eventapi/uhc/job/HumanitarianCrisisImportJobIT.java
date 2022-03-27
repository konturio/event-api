package io.kontur.eventapi.uhc.job;

import static io.kontur.eventapi.uhc.converter.UHCDataLakeConverter.UHC_PROVIDER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.StringInputStream;
import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.uhc.converter.UHCDataLakeConverter;
import io.kontur.eventapi.uhc.service.S3Service;
import io.kontur.eventapi.util.DateTimeUtil;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HumanitarianCrisisImportJobIT {

    private final static String testFile = "events.geojson";

    private final static String emptyTestFile = "empty.geojson";

    private final S3Object s3Object = mock(S3Object.class);

    @Mock
    private DataLakeDao dataLakeDao;

    @Mock
    private S3Service s3Service;

    @Captor
    ArgumentCaptor<ArrayList<DataLake>> dataLakesCaptor;

    @AfterEach
    public void resetMocks() {
        reset(dataLakeDao);
        reset(s3Service);
    }

    @Test
    public void testHumanitarianCrisisDataImport() throws IOException {
        //given
        prepareMocks(testFile);

        //when
        HumanitarianCrisisImportJob importJob = new HumanitarianCrisisImportJob(new SimpleMeterRegistry(), s3Service,
                dataLakeDao, new UHCDataLakeConverter());
        importJob.run();

        //then
        verify(dataLakeDao, times(1))
                .isNewEvent(isA(String.class), isA(String.class), isA(String.class));
        verify(dataLakeDao, times(1)).storeDataLakes(dataLakesCaptor.capture());
        List<ArrayList<DataLake>> dataLakesInv = dataLakesCaptor.getAllValues();
        List<DataLake> dataLakes = dataLakesInv.get(0);
        assertNotNull(dataLakes);
        assertEquals(1, dataLakes.size());
        assertNotNull(dataLakes.get(0));
        DataLake dataLake = dataLakes.get(0);
        assertTrue(StringUtils.isNotBlank(dataLake.getData()));
        assertEquals(UHC_PROVIDER, dataLake.getProvider());
        assertEquals(DateTimeUtil.parseDateTimeByPattern("21-03-2022T00:00:00Z", DateTimeUtil.UHC_DATETIME_PATTERN),
                dataLake.getUpdatedAt());
        assertEquals("ed2daf7d-01a9-42ef-8a30-4b99d40a6f50", dataLake.getExternalId());
    }

    @Test
    public void testHumanitarianCrisisEmptyDataImport() throws IOException {
        //given
        prepareMocks(emptyTestFile);

        //when
        HumanitarianCrisisImportJob importJob = new HumanitarianCrisisImportJob(new SimpleMeterRegistry(), s3Service,
                dataLakeDao, new UHCDataLakeConverter());
        importJob.run();

        //then
        verify(dataLakeDao, times(0))
                .isNewEvent(isA(String.class), isA(String.class), isA(String.class));
        verify(dataLakeDao, times(0)).storeDataLakes(anyList());
    }

    private void prepareMocks(String file) throws IOException {
        when(s3Service.listS3ObjectKeys()).thenReturn(List.of(file));
        when(s3Service.getS3Object(isA(String.class))).thenReturn(s3Object);
        String geoJson = IOUtils.toString(Objects.requireNonNull(this.getClass().getResourceAsStream(file)),
                "UTF-8");
        when(s3Object.getObjectContent())
                .thenReturn(new S3ObjectInputStream(new StringInputStream(geoJson), new HttpGet()));
        when(dataLakeDao.isNewEvent(isA(String.class), isA(String.class), isA(String.class))).thenReturn(true);
        doNothing().when(dataLakeDao).storeDataLakes(anyList());
    }
}