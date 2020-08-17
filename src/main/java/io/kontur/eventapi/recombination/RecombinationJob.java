package io.kontur.eventapi.recombination;

import io.kontur.eventapi.dao.CombinedEventsDao;
import io.kontur.eventapi.dao.NormalizedRecordsDao;
import io.kontur.eventapi.dto.*;
import io.kontur.eventapi.pdc.job.HpSrvSearchJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class RecombinationJob implements Runnable {

//TODO !!!!!!!!!!!!!!!!!!!!!!!!!!REMOVE!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    private static final Logger LOG = LoggerFactory.getLogger(RecombinationJob.class);

    private static final Map<String, EventType> TYPE_MAP = Map.of(
            "FLOOD", EventType.FLOOD,
            "TSUNAMI", EventType.TSUNAMI,
            "TORNADO", EventType.TORNADO,
            "WILDFIRE", EventType.WILDFIRE,
            "WINTERSTORM", EventType.WINTER_STORM
    );

    private static final Map<String, Severity> SEVERITY_MAP = Map.of(
            "WARNING", Severity.EXTREME,
            "WATCH", Severity.SEVERE,
            "ADVISORY", Severity.MODERATE,
            "INFORMATION", Severity.MINOR
    );

    private final NormalizedRecordsDao recordsDao;
    private final CombinedEventsDao combinedEventsDao;

    public RecombinationJob(NormalizedRecordsDao recordsDao, CombinedEventsDao combinedEventsDao) {
        this.recordsDao = recordsDao;
        this.combinedEventsDao = combinedEventsDao;
    }

    @Override
    public void run() {
        List<String> eventsToProcess = getEventsToProcess();
        LOG.info("Recombination job has started. Events to process: {}", eventsToProcess.size());

        for (String eventId : eventsToProcess) {
            List<NormalizedRecordDto> normalizedRecords = getRecordsToCombine(eventId);
            CombinedEventDto event = getEvent(eventId, normalizedRecords);
            normalizedRecords.forEach(record -> event.addEpisode(convertEpisode(record)));
            combinedEventsDao.saveEvent(event);
        }

        LOG.info("Recombination job has finished");
    }

    private List<String> getEventsToProcess() {
        return recordsDao.getEventsIds();
    }

    private List<NormalizedRecordDto> getRecordsToCombine(String eventId) {
        return recordsDao.getRecordsToCombine(eventId);
    }

    private CombinedEventDto getEvent(String eventId, List<NormalizedRecordDto> normalizedRecords) {
        return combinedEventsDao.getEventForExternalId(eventId)
                .orElseGet(() -> createNewCombinedEvent(normalizedRecords));
    }

    private CombinedEventDto createNewCombinedEvent(List<NormalizedRecordDto> normalizedRecords) {
        return normalizedRecords
                .stream()
                .filter(record -> HpSrvSearchJob.HP_SRV_SEARCH_PROVIDER
                        .equals(record.getProvider()))  // at first try to find PDC's hazard
                .findFirst()
                .map(this::convertEvent)
                .orElseGet(() -> convertEvent(normalizedRecords.get(0))); // if no PDC's hazards was found, convert first record
    }

    private CombinedEventDto convertEvent(NormalizedRecordDto recordDto) {
        CombinedEventDto eventDto = new CombinedEventDto();
        eventDto.setName(recordDto.getHazardName());
        eventDto.setDescription(recordDto.getDescription());
        eventDto.setStartedOn(recordDto.getStartedOn());
        eventDto.setEndedOn(recordDto.getEndedOn());
        eventDto.setObservationId(recordDto.getObservationId());
        eventDto.setType(getType(recordDto));
        return eventDto;
    }

    private CombinedEpisodeDto convertEpisode(NormalizedRecordDto recordDto) {
        CombinedEpisodeDto episodeDto = new CombinedEpisodeDto();
        episodeDto.setDescription(recordDto.getDescription());
        episodeDto.setProvider(recordDto.getProvider());
        episodeDto.setLoadedOn(recordDto.getLoadedOn());
        episodeDto.setObservationId(recordDto.getObservationId());
        episodeDto.setOccurredOn(recordDto.getStartedOn());

        CombinedAreaDto areaDto = new CombinedAreaDto();
        areaDto.setSeverity(getSeverity(recordDto));
        areaDto.setGeometry(recordDto.getWktGeometry() == null ? recordDto.getPoint() : recordDto.getWktGeometry());
        episodeDto.addArea(areaDto);

        return episodeDto;
    }

    private EventType getType(NormalizedRecordDto recordDto) {
        EventType type = TYPE_MAP.get(recordDto.getTypeId());
        if (type == null) {
            LOG.warn(String.format(
                    "Unknown hazard type. Observation Id: %s, typeId: %s",
                    recordDto.getObservationId(),
                    recordDto.getTypeId()));
            return EventType.OTHER;
        }
        return type;
    }

    private Severity getSeverity(NormalizedRecordDto recordDto) {
        if (recordDto.getSeverityId() == null) {
            return Severity.UNKNOWN;
        }
        Severity severity = SEVERITY_MAP.get(recordDto.getSeverityId());
        if (severity == null) {
            return Severity.UNKNOWN;
        }
        return severity;
    }
}
