package io.kontur.eventapi.dao;

import io.kontur.eventapi.dao.mapper.MergedGroupsMapper;
import io.kontur.eventapi.entity.MergedGroup;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
public class MergedGroupsDao {
    private final MergedGroupsMapper mapper;

    public MergedGroupsDao(MergedGroupsMapper mapper) {
        this.mapper = mapper;
    }

    public UUID findGroupIdByEvent(UUID eventId) {
        return mapper.findGroupIdByEvent(eventId);
    }

    public BigDecimal getMinPrimaryIdx(UUID mergeGroupId) {
        return mapper.getMinPrimaryIdx(mergeGroupId);
    }

    public List<MergedGroup> getGroup(UUID mergeGroupId) {
        return mapper.getGroup(mergeGroupId);
    }

    public void insert(MergedGroup group) {
        mapper.insert(group);
    }

    public void updateGroupId(UUID fromGroup, UUID toGroup) {
        mapper.updateGroupId(fromGroup, toGroup);
    }

    public void updatePrimaryIdx(UUID mergeGroupId, UUID eventId, BigDecimal idx) {
        mapper.updatePrimaryIdx(mergeGroupId, eventId, idx);
    }
}
