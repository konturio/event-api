package io.kontur.eventapi.pdc.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "pdcMapSrvClient", url = "${pdc.mapSrvHost}")
public interface PdcMapSrvClient {
    @GetMapping("/msf/rest/services/global/pdc_hazard_exposure/MapServer/27/query?where=1%3D1&outFields=*&f=geojson")
    String getExposures();

    @GetMapping("/msf/rest/services/global/pdc_hazard_exposure/MapServer/{serviceNumber}/query?where=1%3D1&outFields=*&f=geojson")
    String getTypeSpecificExposures(@PathVariable String serviceNumber);
}
