package io.kontur.eventapi.staticdata.service;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AwsS3Service {

    private final static Logger LOG = LoggerFactory.getLogger(AwsS3Service.class);

    @Value("${staticdata.s3Bucket}")
    private String bucket;

    @Value("${staticdata.s3Folder}")
    private String folder;

    private final static AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.EU_CENTRAL_1).build();

    public List<String> listS3ObjectKeys() {
        ListObjectsV2Request request = new ListObjectsV2Request().withBucketName(bucket).withPrefix(folder);
        ListObjectsV2Result result = s3.listObjectsV2(request);
        return result.getObjectSummaries()
                .stream()
                .filter(objectSummary -> objectSummary.getSize() > 0)
                .map(S3ObjectSummary::getKey)
                .collect(Collectors.toList());
    }

    public String getS3ObjectContent(String key) throws IOException {
        S3Object object = s3.getObject(bucket, key);
        try (S3ObjectInputStream inputStream = object.getObjectContent()) {
            return new String(inputStream.readAllBytes());
        }
    }

    public Map<String, String> getS3ObjectMetadata(String key) {
        return s3.getObjectMetadata(bucket, key).getUserMetadata();
    }
}
