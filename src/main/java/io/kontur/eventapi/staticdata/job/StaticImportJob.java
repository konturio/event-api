package io.kontur.eventapi.staticdata.job;

import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.staticdata.service.AwsS3Service;
import io.kontur.eventapi.staticdata.service.StaticImportService;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Component
public class StaticImportJob extends AbstractJob {
    private final static Logger LOG = LoggerFactory.getLogger(StaticImportJob.class);

    private final Map<String, StaticImportService> importServices;
    private final AwsS3Service awsS3Service;

    public StaticImportJob(MeterRegistry meterRegistry, Map<String, StaticImportService> importServices,
                           AwsS3Service awsS3Service) {
        super(meterRegistry);
        this.importServices = importServices;
        this.awsS3Service = awsS3Service;
    }

    @Override
    public String getName() {
        return "staticDataImport";
    }

    @Override
    public void execute() throws Exception {
        List<String> keys = awsS3Service.listS3ObjectKeys();
        for (String key : keys) {
            String content = awsS3Service.getS3ObjectContent(key);
            Map<String, String> metadata = awsS3Service.getS3ObjectMetadata(key);
            OffsetDateTime updatedAt = parseDate(metadata.get("updated-at"));
            String provider = metadata.get("provider");
            processFile(parseType(key), provider, updatedAt, content);
        }
    }

    private OffsetDateTime parseDate(String dateString) {
        return dateString == null || dateString.equals("-") ? null : OffsetDateTime.parse(dateString);
    }

    private String parseType(String key) {
        return StringUtils.substringAfterLast(key, ".");
    }

    private void processFile(String type, String provider, OffsetDateTime updatedAt, String content) {
        if (importServices.containsKey(type)) {
            StaticImportService service = importServices.get(type);
            service.saveDataLakes(content, provider, updatedAt);
        } else {
            LOG.error("There is no service to process file of type: " + type);
        }
    }
}
