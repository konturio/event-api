package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.dto.NormalizedRecordDto;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NormalizedRecordsMapper {

    @Insert("<script>" +
            "INSERT INTO normalized_records (" +
            "observation_id, " +
            "provider, " +
            "<if test='wktGeometry != null'>" +
            "  geometry, " +
            "</if>" +
            "app_id, " +
            "autoexpire, " +
            "category_id, " +
            "charter_uri, " +
            "comment_text, " +
            "create_date, " +
            "creator, " +
            "end_date, " +
            "glide_uri, " +
            "hazard_id, " +
            "hazard_name, " +
            "last_update, " +
            "<if test='point != null'>" +
            "  point, " +
            "</if>" +
            "master_incident_id, " +
            "message_id, " +
            "org_id, " +
            "severity_id, " +
            "snc_url, " +
            "start_date, " +
            "status, " +
            "type_id, " +
            "update_date, " +
            "update_user, " +
            "product_total, " +
            "uuid, " +
            "in_dashboard, " +
            "areabrief_url, " +
            "description, " +
            "mag_id, " +
            "mag_uuid, " +
            "mag_create_date, " +
            "mag_update_date, " +
            "title, " +
            "mag_type, " +
            "is_active " +
            ") VALUES (" +
            "#{observationId}, " +
            "#{provider}, " +
            "<if test='wktGeometry != null'>" +
            "  'SRID=4326;${wktGeometry}'::geometry, " +
            "</if>" +
            "#{appId}, " +
            "#{autoexpire}, " +
            "#{categoryId}, " +
            "#{charterUri}, " +
            "#{commentText}, " +
            "#{createDate}, " +
            "#{creator}, " +
            "#{endDate}, " +
            "#{glideUri}, " +
            "#{hazardId}, " +
            "#{hazardName}, " +
            "#{lastUpdate}, " +
            "<if test='point != null'>" +
            "  'SRID=4326;${point}'::geometry, " +
            "</if>" +
            "#{masterIncidentId}, " +
            "#{messageId}, " +
            "#{orgId}, " +
            "#{severityId}, " +
            "#{sncUrl}, " +
            "#{startDate}, " +
            "#{status}, " +
            "#{typeId}, " +
            "#{updateDate}, " +
            "#{updateUser}, " +
            "#{productTotal}, " +
            "#{uuid}, " +
            "#{inDashboard}, " +
            "#{areabriefUrl}, " +
            "#{description}, " +
            "#{magId}, " +
            "#{magUuid}, " +
            "#{magCreateDate}, " +
            "#{magUpdateDate}, " +
            "#{title}, " +
            "#{magType}, " +
            "#{isActive} " +
            ")" +
            "</script>")
    int insertNormalizedRecord(NormalizedRecordDto record);

}
