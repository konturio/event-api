package io.kontur.eventapi.emdat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class GeocoderDto {

    private String name;
    @JsonProperty("osm_id")
    private long osmId;
    private List<BigDecimal> center;
    private List<BigDecimal> bounds;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getOsmId() {
        return osmId;
    }

    public void setOsmId(long osmId) {
        this.osmId = osmId;
    }

    public List<BigDecimal> getCenter() {
        return center;
    }

    public void setCenter(List<BigDecimal> center) {
        this.center = center;
    }

    public List<BigDecimal> getBounds() {
        return bounds;
    }

    public void setBounds(List<BigDecimal> bounds) {
        this.bounds = bounds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GeocoderDto that = (GeocoderDto) o;
        return osmId == that.osmId &&
                Objects.equals(name, that.name) &&
                Objects.equals(center, that.center) &&
                Objects.equals(bounds, that.bounds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, osmId, center, bounds);
    }
}
