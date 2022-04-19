package io.kontur.eventapi.resource.dto;

import java.time.OffsetDateTime;
import java.util.List;

public class DataPaginationDTO {

    public List<EventDto> data;
    public PageMetadata pageMetadata;

    public DataPaginationDTO(List<EventDto> data, OffsetDateTime nextAfterValue) {
        this.data = data;
        this.pageMetadata = new PageMetadata(nextAfterValue);
    }
}