package io.kontur.eventapi.resource.dto;

import io.kontur.eventapi.entity.OpenFeedData;

import java.time.OffsetDateTime;
import java.util.List;

public class DataPaginationDTO {

    public List<OpenFeedData> data;
    public PageMetadata pageMetadata;

    public DataPaginationDTO(List<OpenFeedData> data, OffsetDateTime nextAfterValue) {
        this.data = data;
        this.pageMetadata = new PageMetadata(nextAfterValue);
    }
}