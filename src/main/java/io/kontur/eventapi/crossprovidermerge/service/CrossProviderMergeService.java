package io.kontur.eventapi.crossprovidermerge.service;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import io.kontur.eventapi.crossprovidermerge.client.EventApiClient;
import io.kontur.eventapi.crossprovidermerge.dto.MergePairDecisionDto;
import io.kontur.eventapi.crossprovidermerge.dto.MergePairDto;
import io.kontur.eventapi.util.JsonUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Service
public class CrossProviderMergeService {

    private final EventApiClient eventApiClient;
    private final AmazonS3 s3;
    private final String bucket;
    private final String folder;
    private final Environment environment;

    public CrossProviderMergeService(EventApiClient eventApiClient,
                                     @Value("${crossProviderMerge.s3Bucket:event-api-locker01}") String bucket,
                                     @Value("${crossProviderMerge.s3Folder:cross-provider-merge}") String folder,
                                     Environment environment) {
        this.eventApiClient = eventApiClient;
        this.bucket = bucket;
        this.folder = folder;
        this.environment = environment;
        this.s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.EU_CENTRAL_1).build();
    }

    public MergePairDto getMergePair(List<String> pairId) {
        return eventApiClient.getMergePair(pairId);
    }

    public void saveDecision(MergePairDecisionDto decision) {
        String id1 = decision.getEventId1();
        String id2 = decision.getEventId2();
        if (id1.compareTo(id2) > 0) {
            decision.setEventId1(id2);
            decision.setEventId2(id1);
            id1 = decision.getEventId1();
            id2 = decision.getEventId2();
        }
        String envFolder = getEnvironmentFolder();
        String key = String.format("%s/%s/%s-%s-%s.json", folder, envFolder,
                decision.getDecisionMadeBy(), id1, id2);
        String json = JsonUtil.writeJson(decision);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(bytes.length);
        metadata.setContentType("application/json");
        s3.putObject(bucket, key, new ByteArrayInputStream(bytes), metadata);
    }

    private String getEnvironmentFolder() {
        List<String> profiles = Arrays.asList(environment.getActiveProfiles());
        if (profiles.contains("prod")) {
            return "PROD";
        } else if (profiles.contains("test")) {
            return "TEST";
        } else if (profiles.contains("dev")) {
            return "DEV";
        }
        return "EXP";
    }
}
