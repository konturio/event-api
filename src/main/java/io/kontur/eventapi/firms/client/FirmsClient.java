package io.kontur.eventapi.firms.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(value = "firmsClient", url = "${firms.host}")
public interface FirmsClient {

    @GetMapping("${firms.modis}")
    String getModisData();

    @GetMapping("${firms.suomi}")
    String getSuomiNppVirsData();

    @GetMapping("${firms.noaa}")
    String getNoaa20VirsData();
}
