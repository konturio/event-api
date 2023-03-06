package io.kontur.eventapi.nifc.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(value = "nifcFeignClient", url = "${nifc.host}")
public interface NifcClient {

    @GetMapping("/WFIGS_Interagency_Perimeters_Current/FeatureServer/0/query?outFields=*&where=1%3D1&f=geojson")
    String getNifcPerimeters();

    @GetMapping("/WFIGS_Incident_Locations_Current/FeatureServer/0/query?outFields=*&where=1%3D1&f=geojson")
    String getNifcLocations();
}
