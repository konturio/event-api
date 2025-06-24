package io.kontur.eventapi.resource;

import io.kontur.eventapi.resource.dto.MergeCandidatePairDTO;
import io.kontur.eventapi.service.MergePairService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/v1")
@Hidden
public class MergePairResource {
    private final MergePairService service;

    public MergePairResource(MergePairService service) {
        this.service = service;
    }

    @GetMapping(path = "/merge_pair", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<MergeCandidatePairDTO> getMergePair(@RequestParam(value = "pairID", required = false) List<String> pairID) {
        return pairID != null && pairID.size() == 2 ?
                service.getPair(pairID).map(List::of).orElse(Collections.emptyList()) :
                service.takeNextPair().map(List::of).orElse(Collections.emptyList());
    }
}
