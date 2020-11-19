package io.kontur.eventapi.gdacs.dto;

import java.time.OffsetDateTime;

public class ParsedAlert {
    private OffsetDateTime dateModified;
    private OffsetDateTime sent;
    private String identifier;
    private String eventId;
    private String eventType;
    private String currentEpisodeId;
    private String data;
    private String headLine;
    private String description;
    private OffsetDateTime fromDate;
    private OffsetDateTime toDate;
    private String event;
    private String severity;

    public ParsedAlert() {
    }

    public ParsedAlert(OffsetDateTime dateModified, OffsetDateTime sent, String identifier, String eventId, String eventType, String currentEpisodeId, String data) {
        this.dateModified = dateModified;
        this.sent = sent;
        this.identifier = identifier;
        this.eventId = eventId;
        this.eventType = eventType;
        this.currentEpisodeId = currentEpisodeId;
        this.data = data;
    }

    public OffsetDateTime getDateModified() {
        return dateModified;
    }

    public void setDateModified(OffsetDateTime dateModified) {
        this.dateModified = dateModified;
    }

    public OffsetDateTime getSent() {
        return sent;
    }

    public void setSent(OffsetDateTime sent) {
        this.sent = sent;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getCurrentEpisodeId() {
        return currentEpisodeId;
    }

    public void setCurrentEpisodeId(String currentEpisodeId) {
        this.currentEpisodeId = currentEpisodeId;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getHeadLine() {
        return headLine;
    }

    public void setHeadLine(String headLine) {
        this.headLine = headLine;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public OffsetDateTime getFromDate() {
        return fromDate;
    }

    public void setFromDate(OffsetDateTime fromDate) {
        this.fromDate = fromDate;
    }

    public OffsetDateTime getToDate() {
        return toDate;
    }

    public void setToDate(OffsetDateTime toDate) {
        this.toDate = toDate;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }
}
