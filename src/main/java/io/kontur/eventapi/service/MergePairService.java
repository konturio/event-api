package io.kontur.eventapi.service;

import io.kontur.eventapi.dao.MergePairDao;
import io.kontur.eventapi.resource.dto.MergeCandidatePairDTO;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class MergePairService {

    private final MergePairDao dao;

    public MergePairService(MergePairDao dao) {
        this.dao = dao;
    }

    public List<MergeCandidatePairDTO> getMergePairs(List<String> pairIds) {
        MergeCandidatePairDTO pair;
        if (pairIds != null && pairIds.size() == 2) {
            pair = dao.getPairByIds(pairIds.get(0), pairIds.get(1));
        } else {
            pair = dao.takeNextPair();
        }
        if (pair == null) {
            return Collections.emptyList();
        }
        dao.markAsTaken(pair.getEventId1(), pair.getEventId2());
        return List.of(pair);
    }
}
