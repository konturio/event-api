package io.kontur.eventapi.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class NormalizedRecordDto {

    private UUID observationId;
    private String provider;
    private String wktGeometry;
    private OffsetDateTime loadedOn;
    private Integer appId;
    private Boolean autoexpire;
    private String categoryId;
    private String charterUri;
    private String commentText;
    private OffsetDateTime createdOn;
    private String creator;
    private OffsetDateTime endedOn;
    private String glideUri;
    private String externalId;
    private String hazardName;
    private OffsetDateTime lastUpdatedOn;
    private String point;
    private String masterIncidentId;
    private String messageId;
    private Integer orgId;
    private String severityId;
    private String sncUrl;
    private OffsetDateTime startedOn;
    private String status;
    private String typeId;
    private OffsetDateTime updatedOn;
    private String updateUser;
    private String productTotal;
    private UUID uuid;
    private String inDashboard;
    private String areabriefUrl;
    private String description;
    // HpSrv Mags
    private Integer magId;
    private UUID magUuid;
    private OffsetDateTime magCreatedOn;
    private OffsetDateTime magUpdatedOn;
    private String title;
    private String magType;
    private Boolean isActive;

    public UUID getObservationId() {
        return observationId;
    }

    public void setObservationId(UUID observationId) {
        this.observationId = observationId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getWktGeometry() {
        return wktGeometry;
    }

    public void setWktGeometry(String wktGeometry) {
        this.wktGeometry = wktGeometry;
    }

    public OffsetDateTime getLoadedOn() {
        return loadedOn;
    }

    public void setLoadedOn(OffsetDateTime loadedOn) {
        this.loadedOn = loadedOn;
    }

    public Integer getAppId() {
        return appId;
    }

    public void setAppId(Integer appId) {
        this.appId = appId;
    }

    public Boolean getAutoexpire() {
        return autoexpire;
    }

    public void setAutoexpire(Boolean autoexpire) {
        this.autoexpire = autoexpire;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getCharterUri() {
        return charterUri;
    }

    public void setCharterUri(String charterUri) {
        this.charterUri = charterUri;
    }

    public String getCommentText() {
        return commentText;
    }

    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }

    public OffsetDateTime getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(OffsetDateTime createdOn) {
        this.createdOn = createdOn;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public OffsetDateTime getEndedOn() {
        return endedOn;
    }

    public void setEndedOn(OffsetDateTime endedOn) {
        this.endedOn = endedOn;
    }

    public String getGlideUri() {
        return glideUri;
    }

    public void setGlideUri(String glideUri) {
        this.glideUri = glideUri;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getHazardName() {
        return hazardName;
    }

    public void setHazardName(String hazardName) {
        this.hazardName = hazardName;
    }

    public OffsetDateTime getLastUpdatedOn() {
        return lastUpdatedOn;
    }

    public void setLastUpdatedOn(OffsetDateTime lastUpdatedOn) {
        this.lastUpdatedOn = lastUpdatedOn;
    }

    public String getPoint() {
        return point;
    }

    public void setPoint(String point) {
        this.point = point;
    }

    public String getMasterIncidentId() {
        return masterIncidentId;
    }

    public void setMasterIncidentId(String masterIncidentId) {
        this.masterIncidentId = masterIncidentId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public Integer getOrgId() {
        return orgId;
    }

    public void setOrgId(Integer orgId) {
        this.orgId = orgId;
    }

    public String getSeverityId() {
        return severityId;
    }

    public void setSeverityId(String severityId) {
        this.severityId = severityId;
    }

    public String getSncUrl() {
        return sncUrl;
    }

    public void setSncUrl(String sncUrl) {
        this.sncUrl = sncUrl;
    }

    public OffsetDateTime getStartedOn() {
        return startedOn;
    }

    public void setStartedOn(OffsetDateTime startedOn) {
        this.startedOn = startedOn;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTypeId() {
        return typeId;
    }

    public void setTypeId(String typeId) {
        this.typeId = typeId;
    }

    public OffsetDateTime getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(OffsetDateTime updatedOn) {
        this.updatedOn = updatedOn;
    }

    public String getUpdateUser() {
        return updateUser;
    }

    public void setUpdateUser(String updateUser) {
        this.updateUser = updateUser;
    }

    public String getProductTotal() {
        return productTotal;
    }

    public void setProductTotal(String productTotal) {
        this.productTotal = productTotal;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getInDashboard() {
        return inDashboard;
    }

    public void setInDashboard(String inDashboard) {
        this.inDashboard = inDashboard;
    }

    public String getAreabriefUrl() {
        return areabriefUrl;
    }

    public void setAreabriefUrl(String areabriefUrl) {
        this.areabriefUrl = areabriefUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getMagId() {
        return magId;
    }

    public void setMagId(Integer magId) {
        this.magId = magId;
    }

    public UUID getMagUuid() {
        return magUuid;
    }

    public void setMagUuid(UUID magUuid) {
        this.magUuid = magUuid;
    }

    public OffsetDateTime getMagCreatedOn() {
        return magCreatedOn;
    }

    public void setMagCreatedOn(OffsetDateTime magCreatedOn) {
        this.magCreatedOn = magCreatedOn;
    }

    public OffsetDateTime getMagUpdatedOn() {
        return magUpdatedOn;
    }

    public void setMagUpdatedOn(OffsetDateTime magUpdatedOn) {
        this.magUpdatedOn = magUpdatedOn;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMagType() {
        return magType;
    }

    public void setMagType(String magType) {
        this.magType = magType;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }
}
