package io.kontur.eventapi.emdat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EmDatPublicFile {

    private Long count;
    @JsonProperty("xlsx")
    private String name;

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
