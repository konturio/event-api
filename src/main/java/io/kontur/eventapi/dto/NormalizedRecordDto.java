package io.kontur.eventapi.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class NormalizedRecordDto {

    private UUID observationId;
    private String provider;
    private String wktGeometry;
    private Integer appId;
    private Boolean autoexpire;
    private String categoryId;
    private String charterUri;
    private String commentText;
    private OffsetDateTime createDate;
    private String creator;
    private OffsetDateTime endDate;
    private String glideUri;
    private String hazardId;
    private String hazardName;
    private OffsetDateTime lastUpdate;
    private String point;
    private String masterIncidentId;
    private String messageId;
    private Integer orgId;
    private String severityId;
    private String sncUrl;
    private OffsetDateTime startDate;
    private String status;
    private String typeId;
    private OffsetDateTime updateDate;
    private String updateUser;
    private String productTotal;
    private UUID uuid;
    private String inDashboard;
    private String areabriefUrl;
    private String description;
    // HpSrv Mags
    private Integer magId;
    private UUID magUuid;
    private OffsetDateTime magCreateDate;
    private OffsetDateTime magUpdateDate;
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

    public OffsetDateTime getCreateDate() {
        return createDate;
    }

    public void setCreateDate(OffsetDateTime createDate) {
        this.createDate = createDate;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public OffsetDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(OffsetDateTime endDate) {
        this.endDate = endDate;
    }

    public String getGlideUri() {
        return glideUri;
    }

    public void setGlideUri(String glideUri) {
        this.glideUri = glideUri;
    }

    public String getHazardId() {
        return hazardId;
    }

    public void setHazardId(String hazardId) {
        this.hazardId = hazardId;
    }

    public String getHazardName() {
        return hazardName;
    }

    public void setHazardName(String hazardName) {
        this.hazardName = hazardName;
    }

    public OffsetDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(OffsetDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
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

    public OffsetDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(OffsetDateTime startDate) {
        this.startDate = startDate;
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

    public OffsetDateTime getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(OffsetDateTime updateDate) {
        this.updateDate = updateDate;
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

    public OffsetDateTime getMagCreateDate() {
        return magCreateDate;
    }

    public void setMagCreateDate(OffsetDateTime magCreateDate) {
        this.magCreateDate = magCreateDate;
    }

    public OffsetDateTime getMagUpdateDate() {
        return magUpdateDate;
    }

    public void setMagUpdateDate(OffsetDateTime magUpdateDate) {
        this.magUpdateDate = magUpdateDate;
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
