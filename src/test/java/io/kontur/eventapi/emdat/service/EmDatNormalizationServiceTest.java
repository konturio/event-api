package io.kontur.eventapi.emdat.service;

import io.kontur.eventapi.emdat.client.KonturApiClient;
import io.kontur.eventapi.emdat.dto.GeocoderDto;
import io.kontur.eventapi.util.JsonUtil;
import org.junit.jupiter.api.Test;
import org.wololo.geojson.Geometry;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmDatNormalizationServiceTest {

    @Test
    public void obtaiGeometriesWorksWithoutLocation() {
        //given
        KonturApiClient apiClient = mock(KonturApiClient.class);
        configureSaintVincentCountry(apiClient);

        EmDatNormalizationService service = new EmDatNormalizationService(apiClient);

        //when
        Optional<Geometry> result = service
                .obtainGeometries("Saint Vincent and the Grenadines", null);

        //then
        assertTrue(result.isPresent());
        assertEquals(
                "{\"type\":\"Polygon\",\"coordinates\":[[[-61.6657471,12.5166548],[-60.9094146,12.5166548],[-60.9094146,13.583],[-61.6657471,13.583],[-61.6657471,12.5166548]]]}",
                result.get().toString());
    }


    @Test
    public void obtainGeometries() {
        //given
        KonturApiClient apiClient = mock(KonturApiClient.class);
        configureSaintVincentCountry(apiClient);
        configurePembroke(apiClient);
        configureVermont(apiClient);
        configureBuccamentBay(apiClient);
        configureSandyBay(apiClient);
        configureSpringVillage(apiClient);
        configureByera(apiClient);


        EmDatNormalizationService service = new EmDatNormalizationService(apiClient);

        //when
        Optional<Geometry> result = service
                .obtainGeometries("Saint Vincent and the Grenadines", "Pembroke, Vermont, Buccament Bay villages (Saint Andrew province), Spring Village village (Saint Patrick province), Sandy Bay, Byera");

        //then
        assertTrue(result.isPresent());
        assertEquals(
                "{\"type\":\"MultiPolygon\",\"coordinates\":[[[[-61.257025,13.1905593],[-61.2469463,13.1905593],[-61.2469463,13.1996208],[-61.257025,13.1996208],[-61.257025,13.1905593]]],[[[-61.2712765,13.1610874],[-61.1912765,13.1610874],[-61.1912765,13.2410874],[-61.2712765,13.2410874],[-61.2712765,13.1610874]]],[[[-61.2605311,13.1748605],[-61.2538469,13.1748605],[-61.2538469,13.1818622],[-61.2605311,13.1818622],[-61.2605311,13.1748605]]],[[[-61.2519876,13.2558498],[-61.2429483,13.2558498],[-61.2429483,13.2645111],[-61.2519876,13.2645111],[-61.2519876,13.2558498]]],[[[-61.1349945,13.3587118],[-61.1308363,13.3587118],[-61.1308363,13.3661997],[-61.1349945,13.3661997],[-61.1349945,13.3587118]]]]}",
                result.get().toString());
    }

    private void configureSaintVincentCountry(KonturApiClient apiClient) {
        GeocoderDto geocoderDto = new GeocoderDto();
        geocoderDto.setName("Saint Vincent and the Grenadines");
        geocoderDto.setOsmId(550725);
        geocoderDto.setCenter(List.of(new BigDecimal("-61.2765569"), new BigDecimal("12.90447")));
        geocoderDto.setBounds(List.of(new BigDecimal("-61.6657471"),
                new BigDecimal("12.5166548"),
                new BigDecimal("-60.9094146"),
                new BigDecimal("13.583")));
        when(apiClient.geocoder("Saint Vincent and the Grenadines")).thenReturn(Collections.singletonList(
                geocoderDto));
    }

    private void configurePembroke(KonturApiClient apiClient) {
        GeocoderDto geocoderDto = new GeocoderDto();
        geocoderDto.setName("Pembroke, Vermont, Saint Andrew, Saint Vincent and the Grenadines");
        geocoderDto.setOsmId(384341068);
        geocoderDto.setCenter(List.of(new BigDecimal("-61.2511558406342"), new BigDecimal("13.19510985")));
        geocoderDto.setBounds(List.of(new BigDecimal("-61.257025"),
                new BigDecimal("13.1905593"),
                new BigDecimal("-61.2469463"),
                new BigDecimal("13.1996208")));
        when(apiClient.geocoder("Saint Vincent and the Grenadines Pembroke")).thenReturn(Collections.singletonList(
                geocoderDto));
    }

    private void configureVermont(KonturApiClient apiClient) {
        GeocoderDto geocoderDto = JsonUtil.readJson(
                "{\"name\":\"Vermont, Saint Andrew, VC0100, Saint Vincent and the Grenadines\",\"center\":[-61.2312765,13.2010874],\"osm_id\":5947864404,\"bounds\":[-61.2712765,13.1610874,-61.1912765,13.2410874]}",
                GeocoderDto.class);

        when(apiClient.geocoder("Saint Vincent and the Grenadines Vermont")).thenReturn(Collections.singletonList(
                geocoderDto));
    }

    private void configureBuccamentBay(KonturApiClient apiClient) {
        GeocoderDto geocoderDto = JsonUtil.readJson(
                "{\"name\":\"Clare Valley, Saint Andrew, Saint Vincent and the Grenadines\",\"center\":[-61.25568443901387,13.17834965],\"osm_id\":682519827,\"bounds\":[-61.2605311,13.1748605,-61.2538469,13.1818622]}",
                GeocoderDto.class);

        when(apiClient.geocoder("Saint Vincent and the Grenadines Buccament Bay villages")).thenReturn(Collections.singletonList(
                geocoderDto));
    }

    private void configureSandyBay(KonturApiClient apiClient) {
        GeocoderDto geocoderDto = JsonUtil.readJson(
                "{\"name\":\"Sandy Bay, Old Sandy Bay, Point Village, Charlotte, Saint Vincent and the Grenadines\",\"center\":[-61.132790510767975,13.36251135],\"osm_id\":390083196,\"bounds\":[-61.1349945,13.3587118,-61.1308363,13.3661997]}",
                GeocoderDto.class);

        when(apiClient.geocoder("Saint Vincent and the Grenadines Sandy Bay")).thenReturn(Collections.singletonList(
                geocoderDto));
    }

    private void configureSpringVillage(KonturApiClient apiClient) {
        GeocoderDto geocoderDto = JsonUtil.readJson(
                "{\"name\":\"Spring Village, Cumberland, Barrouallie, Saint Patrick, Saint Vincent and the Grenadines\",\"center\":[-61.2500423840941,13.2598369],\"osm_id\":25818870,\"bounds\":[-61.2519876,13.2558498,-61.2429483,13.2645111]}",
                GeocoderDto.class);

        when(apiClient.geocoder("Saint Vincent and the Grenadines Spring Village village")).thenReturn(Collections.singletonList(geocoderDto));
    }

    private void configureByera(KonturApiClient apiClient) {
        when(apiClient.geocoder("Saint Vincent and the Grenadines Byera")).thenReturn(Collections.emptyList());
    }
}