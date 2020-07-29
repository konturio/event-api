package io.kontur.eventapi.pdc.normalization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kontur.eventapi.dto.EventDataLakeDto;
import io.kontur.eventapi.dto.NormalizedRecordDto;
import io.kontur.eventapi.normalization.Normalizer;
import io.kontur.eventapi.pdc.job.HpSrvSearchJob;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTWriter;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.jts2geojson.GeoJSONReader;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static io.kontur.eventapi.pdc.converter.PdcEventDataLakeConverter.magsDateTimeFormatter;

@Component
public class HpSrvMagsNormalizer extends Normalizer {

    private final ObjectMapper mapper = new ObjectMapper();
    private final GeoJSONReader geoJSONReader = new GeoJSONReader();
    private final WKTWriter wktWriter = new WKTWriter();

    @Override
    public boolean isApplicable(EventDataLakeDto dataLakeDto) {
        return HpSrvSearchJob.HP_SRV_MAG_PROVIDER.equals(dataLakeDto.getProvider());
    }

    @Override
    public NormalizedRecordDto normalize(EventDataLakeDto dataLakeDto) {
        NormalizedRecordDto recordDto = new NormalizedRecordDto();

        recordDto.setObservationId(dataLakeDto.getObservationId());
        recordDto.setProvider(dataLakeDto.getProvider());

        try {
            Feature feature = mapper.readValue(dataLakeDto.getData(), Feature.class);

            Geometry geometry = geoJSONReader.read(feature.getGeometry());
            recordDto.setWktGeometry(wktWriter.write(geometry));

            Map<String, Object> props = feature.getProperties();

            recordDto.setMagId(readInt(props, "magId"));
            String uuid = readString(props, "uuid");
            if (uuid != null) {
                recordDto.setMagUuid(UUID.fromString(uuid));
            }
            recordDto.setTitle(readString(props, "title"));
            recordDto.setMagCreateDate(readDateTime(props, "createDate"));
            recordDto.setMagUpdateDate(readDateTime(props, "updateDate"));
            recordDto.setMagType(readString(props, "magType"));
            recordDto.setCreator(readString(props, "creator"));
            recordDto.setActive(readBoolean(props, "isActive"));
            recordDto.setTypeId(readString(props, "hazard.hazardType.typeId"));
            recordDto.setHazardId(readString(props, "hazard.hazardId"));
            recordDto.setHazardName(readString(props, "hazard.hazardName"));
            recordDto.setCommentText(readString(props, "hazard.commentText"));
            recordDto.setCreateDate(readDateTime(props, "hazard.createDate"));
            recordDto.setStartDate(readDateTime(props, "hazard.startDate"));
            recordDto.setEndDate(readDateTime(props, "hazard.endDate"));
            recordDto.setLastUpdate(readDateTime(props, "hazard.lastUpdate"));
            recordDto.setUpdateDate(readDateTime(props, "hazard.updateDate"));
            recordDto.setPoint(makeWktPoint(readDouble(props, "hazard.longitude"),
                    readDouble(props, "hazard.latitude")));
            recordDto.setOrgId(readInt(props, "hazard.orgId"));
            recordDto.setAutoexpire("Y".equalsIgnoreCase(readString(props, "hazard.autoexpire")));
            recordDto.setMessageId(readString(props, "hazard.messageId"));
            recordDto.setMasterIncidentId(readString(props, "hazard.masterIncidentId"));
            recordDto.setStatus(readString(props, "hazard.status"));
            String hazardUuid = readString(props, "hazard.uuid");
            if (hazardUuid != null) {
                recordDto.setUuid(UUID.fromString(hazardUuid));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return recordDto;
    }

    @Override
    protected OffsetDateTime readDateTime(Map<String, Object> map, String key) {
        String dateTime = readString(map, key);
        return dateTime == null ? null : OffsetDateTime.parse(dateTime, magsDateTimeFormatter);
    }
}
