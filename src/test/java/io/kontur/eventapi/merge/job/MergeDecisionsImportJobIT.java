package io.kontur.eventapi.merge.job;

import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import io.kontur.eventapi.dao.MergeOperationsDao;
import io.kontur.eventapi.merge.service.MergeDecisionsS3Service;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.http.client.methods.HttpGet;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MergeDecisionsImportJobIT {

    @Mock
    private MergeDecisionsS3Service s3Service;
    @Mock
    private MergeOperationsDao mergeOperationsDao;

    @AfterEach
    public void resetMocks() {
        reset(s3Service);
        reset(mergeOperationsDao);
    }

    @Test
    public void testImportNewDecision() throws Exception {
        S3ObjectSummary summary = new S3ObjectSummary();
        summary.setKey("user-1-2.json");
        summary.setLastModified(new Date());
        when(s3Service.listS3ObjectSummaries()).thenReturn(List.of(summary));
        when(mergeOperationsDao.getLastApprovedAt()).thenReturn(null);
        S3Object s3Object = mock(S3Object.class);
        when(s3Service.getS3Object(isA(String.class))).thenReturn(s3Object);
        String json = "[{" +
                "\"approved\":true,\"decisionMadeBy\":\"u\"," +
                "\"eventId1\":\"11111111-1111-1111-1111-111111111111\"," +
                "\"eventId2\":\"22222222-2222-2222-2222-222222222222\"}]";
        when(s3Object.getObjectContent()).thenReturn(new S3ObjectInputStream(IOUtils.toInputStream(json, StandardCharsets.UTF_8), new HttpGet()));

        MergeDecisionsImportJob job = new MergeDecisionsImportJob(new SimpleMeterRegistry(), s3Service, mergeOperationsDao);
        job.run();

        verify(mergeOperationsDao, times(1)).updateMergeDecision(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                true,
                "u",
                any(OffsetDateTime.class));
    }

    @Test
    public void testSkipOldDecision() throws Exception {
        OffsetDateTime last = OffsetDateTime.now();
        S3ObjectSummary summary = new S3ObjectSummary();
        summary.setKey("user-1-2.json");
        summary.setLastModified(Date.from(last.minusDays(1).toInstant()));
        when(s3Service.listS3ObjectSummaries()).thenReturn(List.of(summary));
        when(mergeOperationsDao.getLastApprovedAt()).thenReturn(last);

        MergeDecisionsImportJob job = new MergeDecisionsImportJob(new SimpleMeterRegistry(), s3Service, mergeOperationsDao);
        job.run();

        verify(mergeOperationsDao, never()).updateMergeDecision(any(), any(), any(), any(), any());
    }
}
