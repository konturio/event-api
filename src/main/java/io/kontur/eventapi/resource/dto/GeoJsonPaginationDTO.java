package io.kontur.eventapi.resource.dto;

import lombok.Data;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;

import java.time.OffsetDateTime;

@Data
public class GeoJsonPaginationDTO {
    private String type;
    private Feature[] features;
    private PageMetadata pageMetadata;

    public GeoJsonPaginationDTO(FeatureCollection fc, OffsetDateTime nextAfterValue) {
        this.type = fc.getType();
        this.features = fc.getFeatures();
        this.pageMetadata = new PageMetadata(nextAfterValue);
    }
}
