package io.kontur.eventapi.dto;

import java.time.LocalDateTime;

public class HazardData {

    private String hazardId;
    private LocalDateTime createDate;
    private LocalDateTime uploadDate;
    private String provider;
    private String data;

    public String getHazardId() {
        return hazardId;
    }

    public void setHazardId(String hazardId) {
        this.hazardId = hazardId;
    }

    public LocalDateTime getCreateDate() {
        return createDate;
    }

    public void setCreateDate(LocalDateTime createDate) {
        this.createDate = createDate;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
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
