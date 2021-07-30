package io.kontur.eventapi.enrichment;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InsightsApiRequest {
    private String query;
}
