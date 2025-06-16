package io.kontur.eventapi.resource.internal;

import io.kontur.eventapi.resource.dto.MergeCandidatePairDTO;
import io.kontur.eventapi.service.MergePairService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1")
@Hidden
public class MergePairResource {

    private final MergePairService service;

    public MergePairResource(MergePairService service) {
        this.service = service;
    }

    @GetMapping("/merge_pair")
    public ResponseEntity<List<MergeCandidatePairDTO>> getMergePair(
            @RequestParam(value = "pairID", required = false) List<String> pairId) {
        List<MergeCandidatePairDTO> result = service.getMergePairs(pairId);
        if (result.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(result);
    }
}
