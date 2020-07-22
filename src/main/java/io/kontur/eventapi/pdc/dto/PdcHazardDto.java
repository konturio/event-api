package io.kontur.eventapi.pdc.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PdcHazardDto {

    @JsonProperty("app_ID")
    private String appId;
    @JsonProperty("app_IDs")
    private String appIds;
    @JsonProperty("autoexpire")
    private String autoexpire;
    @JsonProperty("category_ID")
    private String categoryId;
    @JsonProperty("charter_Uri")
    private String charterUri;
    @JsonProperty("comment_Text")
    private String commentText;
    @JsonProperty("create_Date")
    private String createDate;
    @JsonProperty("creator")
    private String creator;
    @JsonProperty("end_Date")
    private String endDate;
    @JsonProperty("glide_Uri")
    private String glideUri;
    @JsonProperty("hazard_ID")
    private String hazardId;
    @JsonProperty("hazard_Name")
    private String hazardName;
    @JsonProperty("last_Update")
    private String lastUpdate;
    @JsonProperty("latitude")
    private String lat;
    @JsonProperty("longitude")
    private String lon;
    @JsonProperty("master_Incident_ID")
    private String masterIncidentId;
    @JsonProperty("message_ID")
    private String messageId;
    @JsonProperty("org_ID")
    private String orgId;
    @JsonProperty("severity_ID")
    private String severityId;
    @JsonProperty("snc_url")
    private String sncUrl;
    @JsonProperty("start_Date")
    private String startDate;
    @JsonProperty("status")
    private String status;
    @JsonProperty("type_ID")
    private String typeId;
    @JsonProperty("update_Date")
    private String updateDate;
    @JsonProperty("update_User")
    private String updateUser;
    @JsonProperty("product_total")
    private String productTotal;
    @JsonProperty("uuid")
    private UUID uuid;
    @JsonProperty("in_Dashboard")
    private String inDashboard;
    @JsonProperty("areabrief_url")
    private String areabriefUrl;
    @JsonProperty("description")
    private String description;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppIds() {
        return appIds;
    }

    public void setAppIds(String appIds) {
        this.appIds = appIds;
    }

    public String getAutoexpire() {
        return autoexpire;
    }

    public void setAutoexpire(String autoexpire) {
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

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
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

    public String getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLon() {
        return lon;
    }

    public void setLon(String lon) {
        this.lon = lon;
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

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
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

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
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

    public String getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(String updateDate) {
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
}
