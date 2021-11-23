package io.kontur.eventapi.nifc.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(value = "nifcFeignClient", url = "${nifc.host}")
public interface NifcClient {

    @GetMapping("/Current_WildlandFire_Perimeters/FeatureServer/0/query?outFields=*&where=1%3D1&f=pgeojson")
    String getNifcPerimeters();

    @GetMapping("/Current_WildlandFire_Locations/FeatureServer/0/query?where=1%3D1&outFields=*&f=pgeojson")
    String getNifcLocations();
}
