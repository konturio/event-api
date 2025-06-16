package io.kontur.eventapi.crossprovidermerge.client;

import io.kontur.eventapi.crossprovidermerge.dto.MergePairDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(value = "eventApiClient", url = "${eventApi.host}")
public interface EventApiClient {

    @GetMapping("/v1/merge_pair")
    MergePairDto getMergePair(@RequestParam("pairID") List<String> pairID);
}
