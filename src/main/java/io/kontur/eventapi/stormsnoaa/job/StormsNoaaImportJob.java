package io.kontur.eventapi.stormsnoaa.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.stormsnoaa.service.StormsNoaaImportService;
import io.kontur.eventapi.stormsnoaa.parser.FileInfo;
import io.kontur.eventapi.stormsnoaa.parser.StormsNoaaHTMLParser;
import io.kontur.eventapi.util.DateTimeUtil;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static io.kontur.eventapi.util.CsvUtil.parseRow;

@Component
public class StormsNoaaImportJob extends AbstractJob {

    private final Logger LOG = LoggerFactory.getLogger(StormsNoaaImportJob.class);
    public final static String STORMS_NOAA_PROVIDER = "storms.noaa";

    private final StormsNoaaHTMLParser htmlParser;
    private final StormsNoaaImportService importService;
    private final DataLakeDao dataLakeDao;

    public StormsNoaaImportJob(MeterRegistry meterRegistry, StormsNoaaHTMLParser htmlParser,
                               StormsNoaaImportService importService, DataLakeDao dataLakeDao) {
        super(meterRegistry);
        this.htmlParser = htmlParser;
        this.importService = importService;
        this.dataLakeDao = dataLakeDao;
    }

    @Override
    public String getName() {
        return "noaaTornado";
    }

    @Override
    public void execute() throws Exception {
        OffsetDateTime latestHazardUpdatedAt = getLatestHazardUpdatedAt();
        List<FileInfo> files = htmlParser.parseFilesInfo().stream()
                .filter(file -> isNewOrUpdatedFile(latestHazardUpdatedAt, file.getUpdatedAt()))
                .sorted(Comparator.comparing(FileInfo::getUpdatedAt))
                .collect(Collectors.toList());
        for (FileInfo file : files) {
            String tmpPath = importService.getFilePath(file.getFilename());
            try {
               importService.downloadFile(file.getFilename(), tmpPath);
               processFile(tmpPath, file.getUpdatedAt());
            } catch (Exception e) {
                LOG.error(e.getMessage());
                break;
            } finally {
                importService.deleteFile(tmpPath);
            }
        }
    }

    private void processFile(String filePath, OffsetDateTime updatedAt) throws Exception {
        try (FileInputStream fileInputStream = new FileInputStream(filePath);
             GZIPInputStream gzipInputStream = new GZIPInputStream(fileInputStream);
             InputStreamReader inputStreamReader = new InputStreamReader(gzipInputStream);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            List<DataLake> dataLakes = new ArrayList<>();
            String header = bufferedReader.readLine();
            String row;
            while ((row = bufferedReader.readLine()) != null) {
                processRow(header, row, updatedAt).ifPresent(dataLakes::add);
            }
            dataLakeDao.storeDataLakes(dataLakes);
        }
    }

    private Optional<DataLake> processRow(String header, String row, OffsetDateTime updatedAt) {
        String externalId = parseRow(header, row).get("EVENT_ID");
        String data = header + "\n" + row;
        if (isNewOrUpdatedEvent(externalId, data) && !externalId.equals("EVENT_ID")) {
            DataLake dataLake = new DataLake(UUID.randomUUID(), externalId, updatedAt, DateTimeUtil.uniqueOffsetDateTime());
            dataLake.setProvider(STORMS_NOAA_PROVIDER);
            dataLake.setData(data);
            return Optional.of(dataLake);
        }
        return Optional.empty();
    }

    private boolean isNewOrUpdatedFile(OffsetDateTime latestHazardUpdatedAt, OffsetDateTime fileUpdatedAt) {
        return latestHazardUpdatedAt == null || !fileUpdatedAt.isBefore(latestHazardUpdatedAt);
    }

    private boolean isNewOrUpdatedEvent(String externalId, String data) {
        Optional<DataLake> existingEvent =  dataLakeDao.getLatestDataLakeByExternalIdAndProvider(externalId, STORMS_NOAA_PROVIDER);
        return existingEvent.isEmpty() || !existingEvent.get().getData().equals(data);
    }

    private OffsetDateTime getLatestHazardUpdatedAt() {
        return dataLakeDao.getLatestUpdatedHazard(STORMS_NOAA_PROVIDER).map(DataLake::getUpdatedAt).orElse(null);
    }
}
