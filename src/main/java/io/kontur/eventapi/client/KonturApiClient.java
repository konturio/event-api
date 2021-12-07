package io.kontur.eventapi.client;

import io.kontur.eventapi.emdat.dto.GeocoderDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.wololo.geojson.FeatureCollection;

import java.util.List;

@FeignClient(value = "konturApiClient", url = "${konturApi.host}")
public interface KonturApiClient {

    @GetMapping("/geocoder/nominatim?limit=1")
    List<GeocoderDto> geocoder(@RequestParam("search") String search);

    @GetMapping("/layers/collections/bounds/itemsByMultipoint?excludeGeometry=true")
    FeatureCollection adminBoundaries(@RequestParam("geom") String geom, @RequestParam("limit") Integer limit);
}
