package io.kontur.eventapi.resource.dto;

import java.util.List;

public class DataPaginationDTO {

    public List<EventDto> data;
    public Pagination page;

    public DataPaginationDTO(List<EventDto> data, int totalElements, int offset) {
        this.data = data;
        this.page = new Pagination(data.size(), totalElements, offset);
    }

    public static class Pagination {
        public int pageSize;
        public int totalElements;
        public int offset;

        public Pagination(int pageSize, int totalElements, int offset) {
            this.pageSize = pageSize;
            this.totalElements = totalElements;
            this.offset = offset;
        }
    }

}
