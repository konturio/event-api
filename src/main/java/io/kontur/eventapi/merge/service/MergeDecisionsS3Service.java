package io.kontur.eventapi.merge.service;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class MergeDecisionsS3Service {

    @Value("${mergeDecisions.s3Bucket}")
    private String bucket;

    @Value("${mergeDecisions.s3Folder}")
    private String folder;

    private static final AmazonS3 s3 = AmazonS3ClientBuilder.standard()
            .withRegion(Regions.EU_CENTRAL_1)
            .build();

    public List<S3ObjectSummary> listS3ObjectSummaries() {
        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(bucket)
                .withPrefix(folder);
        ListObjectsV2Result result = s3.listObjectsV2(request);
        return result.getObjectSummaries().stream()
                .filter(summary -> summary.getSize() > 0)
                .collect(Collectors.toList());
    }

    public S3Object getS3Object(String key) {
        return s3.getObject(new GetObjectRequest(bucket, key));
    }
}
