package io.kontur.eventapi.client;

import io.kontur.eventapi.emdat.dto.GeocoderDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.wololo.geojson.FeatureCollection;

import java.util.List;
import java.util.Map;

@FeignClient(value = "konturApiClient", url = "${konturApi.host}")
public interface KonturApiClient {

    @GetMapping("/geocoder/nominatim?limit=1")
    List<GeocoderDto> geocoder(@RequestParam("search") String search);

    @PostMapping("/layers/v2/collections/konturBoundaries/items/search")
    FeatureCollection adminBoundaries(@RequestBody Map<String, Object> params);
}
