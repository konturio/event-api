package io.kontur.eventapi.job;

import io.kontur.eventapi.dao.FeedEventStatusDao;
import io.kontur.eventapi.dao.MergeOperationsDao;
import io.kontur.eventapi.dao.MergedGroupsDao;
import io.kontur.eventapi.entity.MergeOperation;
import io.kontur.eventapi.entity.MergedGroup;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
public class MergeRecalculationJob extends AbstractJob {

    private static final Logger LOG = LoggerFactory.getLogger(MergeRecalculationJob.class);

    private final MergeOperationsDao operationsDao;
    private final MergedGroupsDao groupsDao;
    private final FeedEventStatusDao feedEventStatusDao;

    public MergeRecalculationJob(MeterRegistry meterRegistry,
                                 MergeOperationsDao operationsDao,
                                 MergedGroupsDao groupsDao,
                                 FeedEventStatusDao feedEventStatusDao) {
        super(meterRegistry);
        this.operationsDao = operationsDao;
        this.groupsDao = groupsDao;
        this.feedEventStatusDao = feedEventStatusDao;
    }

    @Override
    public void execute() {
        List<MergeOperation> operations = operationsDao.getPendingOperations();
        for (MergeOperation operation : operations) {
            processOperation(operation);
            operationsDao.markExecuted(operation.getOperationId());
        }
    }

    private void processOperation(MergeOperation operation) {
        UUID group1 = groupsDao.findGroupIdByEvent(operation.getEventId1());
        UUID group2 = groupsDao.findGroupIdByEvent(operation.getEventId2());

        if (Boolean.TRUE.equals(operation.getApproved())) {
            mergeApproved(operation, group1, group2);
        } else if (Boolean.FALSE.equals(operation.getApproved())) {
            mergeRejected(operation, group1, group2);
        }
    }

    private void mergeApproved(MergeOperation op, UUID g1, UUID g2) {
        if (g1 != null && g2 != null && !g1.equals(g2)) {
            groupsDao.updateGroupId(g2, g1);
        } else if (g1 != null) {
            BigDecimal min = groupsDao.getMinPrimaryIdx(g1);
            MergedGroup mg = new MergedGroup();
            mg.setMergeGroupId(g1);
            mg.setEventId(op.getEventId2());
            mg.setPrimaryIdx(min.subtract(BigDecimal.ONE));
            groupsDao.insert(mg);
        } else if (g2 != null) {
            BigDecimal min = groupsDao.getMinPrimaryIdx(g2);
            MergedGroup mg = new MergedGroup();
            mg.setMergeGroupId(g2);
            mg.setEventId(op.getEventId1());
            mg.setPrimaryIdx(min.subtract(BigDecimal.ONE));
            groupsDao.insert(mg);
        } else {
            UUID groupId = UUID.randomUUID();
            MergedGroup mg1 = new MergedGroup();
            MergedGroup mg2 = new MergedGroup();
            mg1.setMergeGroupId(groupId);
            mg2.setMergeGroupId(groupId);
            if (Math.random() > 0.5) {
                mg1.setEventId(op.getEventId1());
                mg1.setPrimaryIdx(BigDecimal.ONE);
                mg2.setEventId(op.getEventId2());
                mg2.setPrimaryIdx(BigDecimal.ZERO);
            } else {
                mg1.setEventId(op.getEventId1());
                mg1.setPrimaryIdx(BigDecimal.ZERO);
                mg2.setEventId(op.getEventId2());
                mg2.setPrimaryIdx(BigDecimal.ONE);
            }
            groupsDao.insert(mg1);
            groupsDao.insert(mg2);
        }
        markNonActual(g1 != null ? g1 : (g2 != null ? g2 : null));
    }

    private void mergeRejected(MergeOperation op, UUID g1, UUID g2) {
        if (g1 != null && g1.equals(g2)) {
            UUID newGroup = UUID.randomUUID();
            groupsDao.updateGroupId(g1, newGroup);
        }
        markNonActual(g1);
        markNonActual(g2);
    }

    private void markNonActual(UUID groupId) {
        if (groupId == null) {
            return;
        }
        List<MergedGroup> groups = groupsDao.getGroup(groupId);
        for (MergedGroup g : groups) {
            try {
                feedEventStatusDao.markAsNonActual("merge", g.getEventId());
            } catch (Exception e) {
                LOG.warn("Failed to mark event {} non actual", g.getEventId(), e);
            }
        }
    }

    @Override
    public String getName() {
        return "mergeRecalculationJob";
    }
}
