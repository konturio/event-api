package io.kontur.eventapi.merge.job;

import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import io.kontur.eventapi.dao.MergeOperationsDao;
import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.merge.dto.MergeDecisionDTO;
import io.kontur.eventapi.merge.service.MergeDecisionsS3Service;
import io.kontur.eventapi.util.JsonUtil;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class MergeDecisionsImportJob extends AbstractJob {

    private static final Logger LOG = LoggerFactory.getLogger(MergeDecisionsImportJob.class);

    private final MergeDecisionsS3Service s3Service;
    private final MergeOperationsDao mergeOperationsDao;

    public MergeDecisionsImportJob(MeterRegistry meterRegistry,
                                   MergeDecisionsS3Service s3Service,
                                   MergeOperationsDao mergeOperationsDao) {
        super(meterRegistry);
        this.s3Service = s3Service;
        this.mergeOperationsDao = mergeOperationsDao;
    }

    @Override
    public String getName() {
        return "mergeDecisionsImport";
    }

    @Override
    public void execute() throws Exception {
        OffsetDateTime lastApprovedAt = mergeOperationsDao.getLastApprovedAt();
        List<S3ObjectSummary> summaries = s3Service.listS3ObjectSummaries();
        for (S3ObjectSummary summary : summaries) {
            OffsetDateTime modifiedAt = OffsetDateTime.ofInstant(summary.getLastModified().toInstant(), ZoneOffset.UTC);
            if (lastApprovedAt != null && !modifiedAt.isAfter(lastApprovedAt)) {
                continue;
            }
            try (S3Object s3Object = s3Service.getS3Object(summary.getKey())) {
                processFile(modifiedAt, s3Object.getObjectContent());
            } catch (Exception e) {
                LOG.warn("Error while processing merge decision file {}", summary.getKey(), e);
            }
        }
    }

    void processFile(OffsetDateTime modifiedAt, InputStream content) throws Exception {
        String json = IOUtils.toString(content, StandardCharsets.UTF_8);
        List<MergeDecisionDTO> decisions = JsonUtil.readJson(json,
                new com.fasterxml.jackson.core.type.TypeReference<List<MergeDecisionDTO>>() {});
        if (CollectionUtils.isEmpty(decisions)) {
            return;
        }
        for (MergeDecisionDTO dto : decisions) {
            mergeOperationsDao.updateMergeDecision(dto.getEventId1(), dto.getEventId2(),
                    dto.getApproved(), dto.getDecisionMadeBy(), modifiedAt);
        }
    }
}
