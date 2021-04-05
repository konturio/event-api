package io.kontur.eventapi.staticdata.job;

import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.staticdata.reader.StaticFileReader;
import io.kontur.eventapi.staticdata.service.StaticImportService;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.List;
import java.util.Map;

@Component
public class StaticImportJob extends AbstractJob {

    private final static Logger LOG = LoggerFactory.getLogger(StaticImportJob.class);
    private String STATIC_DATA_FOLDER = "static/";
    
    private final Map<String, StaticImportService> importServices;
    private final StaticFileReader fileReader;

    public StaticImportJob(MeterRegistry meterRegistry, Map<String, StaticImportService> importServices,
                           StaticFileReader fileReader) {
        super(meterRegistry);
        this.importServices = importServices;
        this.fileReader = fileReader;
    }

    @Override
    public String getName() {
        return "staticDataImport";
    }

    @Override
    public void execute() throws Exception {
        List<String> filenames = fileReader.findAllFilenames(STATIC_DATA_FOLDER);
        for (String filename : filenames) {
            try {
                String data = fileReader.readFile(STATIC_DATA_FOLDER + filename);
                String provider = parseProvider(filename);
                OffsetDateTime updatedAt = parseUpdatedAt(filename);
                String fileType = parseFileType(filename);
                findService(fileType).saveDataLakes(data, provider, updatedAt);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }
    
    private String parseProvider(String filename) {
        return StringUtils.substringBetween(filename, "[", "]");
    }

    private OffsetDateTime parseUpdatedAt(String filename) {
        String updatedAt = StringUtils.substringBetween(filename, "(", ")");
        return updatedAt == null ? null : OffsetDateTime.of(LocalDateTime.parse(updatedAt), ZoneOffset.UTC);
    }

    private String parseFileType(String filename) {
        return StringUtils.substringAfterLast(filename, ".");
    }

    private StaticImportService findService(String fileType) throws Exception {
        if (importServices.containsKey(fileType)) {
            return importServices.get(fileType);
        }
        throw new Exception("There is no service to process file of type: " + fileType);
    }
}
