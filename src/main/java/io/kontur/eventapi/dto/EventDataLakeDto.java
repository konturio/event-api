package io.kontur.eventapi.dto;

import java.time.OffsetDateTime;
import java.util.UUID;




public class EventDataLakeDto {

    private UUID observationId;
    private String hazardId;
    private OffsetDateTime createDate;
    private OffsetDateTime updateDate;
    private OffsetDateTime uploadDate;
    private String provider;
    private String data;

    public UUID getObservationId() {
        return observationId;
    }

    public void setObservationId(UUID observationId) {
        this.observationId = observationId;
    }

    public String getHazardId() {
        return hazardId;
    }

    public void setHazardId(String hazardId) {
        this.hazardId = hazardId;
    }

    public OffsetDateTime getCreateDate() {
        return createDate;
    }

    public void setCreateDate(OffsetDateTime createDate) {
        this.createDate = createDate;
    }

    public OffsetDateTime getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(OffsetDateTime updateDate) {
        this.updateDate = updateDate;
    }

    public OffsetDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(OffsetDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
