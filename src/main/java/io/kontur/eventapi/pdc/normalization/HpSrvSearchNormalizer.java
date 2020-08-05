package io.kontur.eventapi.pdc.normalization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kontur.eventapi.dto.EventDataLakeDto;
import io.kontur.eventapi.dto.NormalizedRecordDto;
import io.kontur.eventapi.normalization.Normalizer;
import io.kontur.eventapi.pdc.job.HpSrvSearchJob;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class HpSrvSearchNormalizer extends Normalizer {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public boolean isApplicable(EventDataLakeDto dataLakeDto) {
        return HpSrvSearchJob.HP_SRV_SEARCH_PROVIDER.equals(dataLakeDto.getProvider());
    }

    @Override
    public NormalizedRecordDto normalize(EventDataLakeDto dataLakeDto) {
        NormalizedRecordDto recordDto = new NormalizedRecordDto();

        recordDto.setObservationId(dataLakeDto.getObservationId());
        recordDto.setProvider(dataLakeDto.getProvider());
        recordDto.setLoadedOn(dataLakeDto.getLoadedOn());

        try {
            Map<String, Object> props = mapper.readValue(dataLakeDto.getData(), new TypeReference<>() {});
            recordDto.setAppId(readInt(props, "app_ID"));
            recordDto.setAutoexpire("Y".equalsIgnoreCase(readString(props, "autoexpire")));
            recordDto.setCategoryId(readString(props, "category_ID"));
            recordDto.setCharterUri(readString(props, "charter_Uri"));
            recordDto.setCommentText(readString(props, "comment_Text"));
            recordDto.setCreator(readString(props, "creator"));
            recordDto.setGlideUri(readString(props, "glide_Uri"));
            recordDto.setExternalId(readString(props, "hazard_ID"));
            recordDto.setHazardName(readString(props, "hazard_Name"));
            recordDto.setMasterIncidentId(readString(props, "master_Incident_ID"));
            recordDto.setMessageId(readString(props, "message_ID"));
            recordDto.setOrgId(readInt(props, "org_ID"));
            recordDto.setSeverityId(readString(props, "severity_ID"));
            recordDto.setSncUrl(readString(props, "snc_url"));
            recordDto.setStatus(readString(props, "status"));
            recordDto.setTypeId(readString(props, "type_ID"));
            recordDto.setUpdateUser(readString(props, "update_User"));
            recordDto.setProductTotal(readString(props, "product_total"));
            String uuid = readString(props, "uuid");
            if (uuid != null) {
                recordDto.setUuid(UUID.fromString(uuid));
            }
            recordDto.setInDashboard(readString(props, "in_Dashboard"));
            recordDto.setAreabriefUrl(readString(props, "areabrief_url"));
            recordDto.setDescription(readString(props, "description"));

            recordDto.setCreatedOn(readDateTime(props, "create_Date"));
            recordDto.setEndedOn(readDateTime(props, "end_Date"));
            recordDto.setLastUpdatedOn(readDateTime(props, "last_Update"));
            recordDto.setStartedOn(readDateTime(props, "start_Date"));
            recordDto.setUpdatedOn(readDateTime(props, "update_Date"));

            recordDto.setPoint(makeWktPoint(readDouble(props, "longitude"), readDouble(props, "latitude")));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return recordDto;
    }

}
