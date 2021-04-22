package io.kontur.eventapi.stormsnoaa.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.stormsnoaa.client.StormsNoaaClient;
import io.kontur.eventapi.stormsnoaa.parser.FileInfo;
import io.kontur.eventapi.stormsnoaa.parser.StormsNoaaHTMLParser;
import io.kontur.eventapi.util.DateTimeUtil;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static io.kontur.eventapi.util.CsvUtil.parseRow;

@Component
public class StormsNoaaImportJob extends AbstractJob {

    public final static String STORMS_NOAA_PROVIDER = "storms.noaa";

    private final StormsNoaaHTMLParser htmlParser;
    private final StormsNoaaClient client;
    private final DataLakeDao dataLakeDao;

    public StormsNoaaImportJob(MeterRegistry meterRegistry, StormsNoaaHTMLParser htmlParser,
                               StormsNoaaClient client, DataLakeDao dataLakeDao) {
        super(meterRegistry);
        this.htmlParser = htmlParser;
        this.client = client;
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
            processFile(file.getFilename(), file.getUpdatedAt());
        }
    }

    private void processFile(String filename, OffsetDateTime updatedAt) throws Exception {
            String csv = decompressGZIP(client.getGZIP(filename));
            String[] rows = StringUtils.split(csv, "\n");
            String header = rows[0];
            List<DataLake> dataLakes = new ArrayList<>();
            for (int i = 1; i < rows.length; i++) {
                processRow(header, rows[i], updatedAt).ifPresent(dataLakes::add);
            }
            dataLakeDao.storeDataLakes(dataLakes);

    }

    private Optional<DataLake> processRow(String header, String row, OffsetDateTime updatedAt) {
        String externalId = parseRow(header, row).get("EVENT_ID");
        String data = header + "\n" + row;
        if (isNewOrUpdatedEvent(externalId, data)) {
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
        Optional<DataLake> existingEvent =  dataLakeDao.getDataLakeByExternalIdAndProvider(externalId, STORMS_NOAA_PROVIDER);
        return existingEvent.isEmpty() || !existingEvent.get().getData().equals(data);
    }

    private OffsetDateTime getLatestHazardUpdatedAt() {
        return dataLakeDao.getLatestUpdatedHazard(STORMS_NOAA_PROVIDER).map(DataLake::getUpdatedAt).orElse(null);
    }

    private String decompressGZIP(byte[] gzip) throws IOException{
        GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(gzip));
        return new String(gzipInputStream.readAllBytes());
    }
}
