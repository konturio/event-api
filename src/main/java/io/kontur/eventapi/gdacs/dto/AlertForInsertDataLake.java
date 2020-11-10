package io.kontur.eventapi.gdacs.dto;

import java.time.OffsetDateTime;

public class AlertForInsertDataLake {

    private OffsetDateTime updateDate;
    private String externalId;
    private String data;
    private OffsetDateTime sentDateTime;

    public AlertForInsertDataLake(OffsetDateTime updateDate, String externalId, String data, OffsetDateTime sentDateTime) {
        this.updateDate = updateDate;
        this.externalId = externalId;
        this.data = data;
        this.sentDateTime = sentDateTime;
    }

    public OffsetDateTime getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(OffsetDateTime updateDate) {
        this.updateDate = updateDate;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public OffsetDateTime getSentDateTime() {
        return sentDateTime;
    }

    public void setSentDateTime(OffsetDateTime sentDateTime) {
        this.sentDateTime = sentDateTime;
    }
}
