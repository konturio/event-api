package io.kontur.eventapi.crossprovidermerge.resource;

import io.kontur.eventapi.crossprovidermerge.dto.MergePairDecisionDto;
import io.kontur.eventapi.crossprovidermerge.dto.MergePairDto;
import io.kontur.eventapi.crossprovidermerge.service.CrossProviderMergeService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/merge_pair")
public class CrossProviderMergeResource {

    private final CrossProviderMergeService service;

    public CrossProviderMergeResource(CrossProviderMergeService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Select event pair")
    public MergePairDto getMergePair(@RequestParam("pairID") List<String> pairID) {
        return service.getMergePair(pairID);
    }

    @PostMapping
    @Operation(summary = "Save decision on merge pair")
    public void saveDecision(@RequestBody MergePairDecisionDto decision) {
        service.saveDecision(decision);
    }
}
