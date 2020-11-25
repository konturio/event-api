package io.kontur.eventapi.firms.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(value = "firmsClient", url = "https://firms.modaps.eosdis.nasa.gov/data/active_fire/")
public interface FirmsClient {

    @GetMapping("/c6/csv/MODIS_C6_Global_24h.csv")
    String getModisData();

    @GetMapping("/suomi-npp-viirs-c2/csv/SUOMI_VIIRS_C2_Global_24h.csv")
    String getSuomiNppVirsData();

    @GetMapping("/noaa-20-viirs-c2/csv/J1_VIIRS_C2_Global_24h.csv")
    String getNoaa20VirsData();
}
