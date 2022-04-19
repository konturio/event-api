package io.kontur.eventapi.resource.dto;

import lombok.Data;
import org.wololo.geojson.FeatureCollection;

import java.time.OffsetDateTime;

@Data
public class GeoJsonPaginationDTO {
    private FeatureCollection data;
    private PageMetadata pageMetadata;

    public GeoJsonPaginationDTO(FeatureCollection data, OffsetDateTime nextAfterValue) {
        this.data = data;
        this.pageMetadata = new PageMetadata(nextAfterValue);
    }
}
