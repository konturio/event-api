package io.kontur.eventapi.enrichment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InsightsApiRequest {
    private String query;
    private Object variables;
}
