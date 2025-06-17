package io.kontur.eventapi.pdc.service;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Save PDC products to S3 bucket avoiding duplicates.
 */
@Component
public class PdcProductS3Service {

    @Value("${pdcProduct.s3Bucket}")
    private String bucket;

    @Value("${pdcProduct.s3Folder}")
    private String folder;

    private static final AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.EU_CENTRAL_1).build();

    /**
     * Store product JSON if it is not present yet.
     */
    public void saveProduct(String productId, String data) {
        String key = folder + productId + ".json";
        if (!s3.doesObjectExist(bucket, key)) {
            byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(bytes.length);
            s3.putObject(new PutObjectRequest(bucket, key, new ByteArrayInputStream(bytes), metadata));
        }
    }
}
