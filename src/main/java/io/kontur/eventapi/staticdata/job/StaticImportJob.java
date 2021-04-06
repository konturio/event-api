package io.kontur.eventapi.staticdata.job;

import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.staticdata.config.StaticFileData;
import io.kontur.eventapi.staticdata.service.StaticImportService;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Component
public class StaticImportJob extends AbstractJob {

    private final static Logger LOG = LoggerFactory.getLogger(StaticImportJob.class);

    private final Map<String, StaticImportService> importServices;
    private List<StaticFileData> files;

    public StaticImportJob(MeterRegistry meterRegistry, Map<String, StaticImportService> importServices,
                           List<StaticFileData> files) {
        super(meterRegistry);
        this.importServices = importServices;
        this.files = files;
    }

    @Override
    public String getName() {
        return "staticDataImport";
    }

    @Override
    public void execute() throws Exception {
        for (StaticFileData file : files) {
            try {
                String data = readFile(file.getPath());
                StaticImportService service = findService(file.getType());
                service.saveDataLakes(data, file.getProvider(), file.getUpdatedAt());
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    private String readFile(String filePath) throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath);
        if (inputStream == null) {
            throw new IOException("File not found: " + filePath);
        }
        return new String(inputStream.readAllBytes());
    }

    private StaticImportService findService(String fileType) throws Exception {
        if (importServices.containsKey(fileType)) {
            return importServices.get(fileType);
        }
        throw new Exception("There is no service to process file of type: " + fileType);
    }
}
