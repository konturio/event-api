package io.kontur.eventapi.emdat.client;

import io.kontur.eventapi.emdat.dto.GeocoderDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(value = "konturApiClient", url = "${konturApi.host}")
public interface KonturApiClient {

    @GetMapping("/geocoder/nominatim?limit=1")
    List<GeocoderDto> geocoder(@RequestParam("search") String search);
}
