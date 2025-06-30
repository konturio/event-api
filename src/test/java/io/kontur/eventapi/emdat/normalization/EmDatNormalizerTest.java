package io.kontur.eventapi.emdat.normalization;

import io.kontur.eventapi.client.KonturApiClient;
import io.kontur.eventapi.emdat.jobs.EmDatImportJob;
import io.kontur.eventapi.emdat.normalization.converter.CycloneSeverityConverter;
import io.kontur.eventapi.emdat.normalization.converter.EarthquakeSeverityConverter;
import io.kontur.eventapi.emdat.normalization.converter.EmDatGeometryConverter;
import io.kontur.eventapi.emdat.normalization.converter.EmDatSeverityConverter;
import io.kontur.eventapi.emdat.service.EmDatNormalizationService;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.util.SeverityUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmDatNormalizerTest {

    private static final String HEADER = "Dis No,Year,Seq,Disaster Group,Disaster Subgroup,Disaster Type,Disaster Subtype,Disaster Subsubtype,Event Name,Entry Criteria,Country,ISO,Region,Continent,Location,Origin,Associated Dis,Associated Dis2,OFDA Response,Appeal,Declaration,Aid Contribution,Dis Mag Value,Dis Mag Scale,Latitude,Longitude,Local Time,River Basin,Start Year,Start Month,Start Day,End Year,End Month,End Day,Total Deaths,No Injured,No Affected,No Homeless,Total Affected,Reconstruction Costs ('000 US$),Insured Damages ('000 US$),Total Damages ('000 US$),CPI";

    @Mock
    private KonturApiClient apiClient;

    private EmDatNormalizer createNormalizer() {
        when(apiClient.geocoder(anyString())).thenReturn(Collections.emptyList());
        EmDatNormalizationService service = new EmDatNormalizationService(apiClient);
        return new EmDatNormalizer(
                List.of(new CycloneSeverityConverter(), new EarthquakeSeverityConverter(), new EmDatSeverityConverter()),
                new EmDatGeometryConverter(), service);
    }

    @Test
    void windSpeedSeverityData() {
        EmDatNormalizer normalizer = createNormalizer();
        String row = "2020-0207-CUB,2020,0207,Natural,Meteorological,Storm,Convective storm,,,Affected,Cuba,CUB,Caribbean,Americas,\"Sancti Spiritus, Villa Clara, Cienfuegos, Camaguey, Ciego de Avila provinces (central Cuba)\",,Flood,,,,,,120,Kph,,,,,2020,5,20,2020,5,25,,3,3830,,3833";
        DataLake dl = new DataLake();
        dl.setObservationId(UUID.randomUUID());
        dl.setExternalId("2020-0207-CUB");
        dl.setProvider(EmDatImportJob.EM_DAT_PROVIDER);
        dl.setData(HEADER + "\n" + row);
        dl.setLoadedAt(OffsetDateTime.now());
        dl.setUpdatedAt(OffsetDateTime.now());

        NormalizedObservation obs = normalizer.normalize(dl);

        Map<String, Object> sd = obs.getSeverityData();
        assertEquals(120.0, sd.get(SeverityUtil.WIND_SPEED_KPH));
        assertEquals(SeverityUtil.getCycloneCategory(120.0), sd.get(SeverityUtil.CATEGORY_SAFFIR_SIMPSON));
    }

    @Test
    void magnitudeSeverityData() {
        EmDatNormalizer normalizer = createNormalizer();
        String row = "2021-0001-ABC,2021,0001,Natural,Geophysical,Earthquake,,,Test event,,TestCountry,TCO,Region,Continent,Location,,,,,,,,5,Richter,10,20,,,2021,1,2,2021,1,3,,,,,,,";
        DataLake dl = new DataLake();
        dl.setObservationId(UUID.randomUUID());
        dl.setExternalId("2021-0001-ABC");
        dl.setProvider(EmDatImportJob.EM_DAT_PROVIDER);
        dl.setData(HEADER + "\n" + row);
        dl.setLoadedAt(OffsetDateTime.now());
        dl.setUpdatedAt(OffsetDateTime.now());

        NormalizedObservation obs = normalizer.normalize(dl);

        Map<String, Object> sd = obs.getSeverityData();
        assertEquals(5.0, sd.get(SeverityUtil.MAGNITUDE));
    }
}
