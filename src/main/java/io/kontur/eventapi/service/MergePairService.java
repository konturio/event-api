package io.kontur.eventapi.service;

import io.kontur.eventapi.dao.ApiDao;
import io.kontur.eventapi.dao.KonturEventsDao;
import io.kontur.eventapi.dao.MergeOperationsDao;
import io.kontur.eventapi.entity.MergeOperation;
import io.kontur.eventapi.resource.dto.*;
import io.kontur.eventapi.resource.dto.EpisodeDto;
import io.kontur.eventapi.util.JsonUtil;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MergePairService {
    private final MergeOperationsDao operationsDao;
    private final KonturEventsDao eventsDao;
    private final ApiDao apiDao;

    private static final String FEED_ALIAS = "test-cross-provider-merge";

    public MergePairService(MergeOperationsDao operationsDao, KonturEventsDao eventsDao, ApiDao apiDao) {
        this.operationsDao = operationsDao;
        this.eventsDao = eventsDao;
        this.apiDao = apiDao;
    }

    public Optional<MergeCandidatePairDTO> takeNextPair() {
        return operationsDao.takeNext().flatMap(this::toDto);
    }

    public Optional<MergeCandidatePairDTO> getPair(List<String> pair) {
        return operationsDao.getByEventIds(pair).flatMap(this::toDto);
    }

    private Optional<MergeCandidatePairDTO> toDto(MergeOperation op) {
        if (op == null || op.getEventIds().size() != 2) {
            return Optional.empty();
        }
        EventDto e1 = fetchEvent(op.getEventIds().get(0));
        EventDto e2 = fetchEvent(op.getEventIds().get(1));
        if (e1 == null || e2 == null) {
            return Optional.empty();
        }
        MergeCandidatePairDTO dto = new MergeCandidatePairDTO();
        dto.setEventId1(op.getEventIds().get(0));
        dto.setEventId2(op.getEventIds().get(1));
        dto.setConfidence(op.getConfidence());
        dto.setApproved(op.getApproved());
        dto.setDecisionMadeBy(op.getDecisionMadeBy());
        dto.setDecisionMadeAt(op.getDecisionMadeAt());
        dto.setEvent1(e1);
        dto.setEvent2(e2);
        return Optional.of(dto);
    }

    private EventDto fetchEvent(String externalId) {
        return eventsDao.getEventByExternalId(externalId)
                .flatMap(ev -> apiDao.getEventByEventIdAndByVersionOrLast(ev.getEventId(), FEED_ALIAS, null,
                        EpisodeFilterType.ANY, GeometryFilterType.ANY))
                .map(json -> JsonUtil.readJson(json, EventDto.class))
                .orElse(null);
    }
}
