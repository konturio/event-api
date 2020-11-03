package io.kontur.eventapi.viirs.jobs;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.util.DateTimeUtil;
import io.kontur.eventapi.viirs.client.FirmsClient;
import io.micrometer.core.annotation.Timed;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class FirmsImportJob implements Runnable {

    private final static Logger LOG = LoggerFactory.getLogger(FirmsImportJob.class);

    private final static String MODIS_PROVIDER = "firms.modis-c6";
    private final static String SUOMI_PROVIDER = "firms.suomi-npp-viirs-c2";
    private final static String NOAA_PROVIDER = "firms.noaa-20-viirs-c2";

    public static final String CSV_SEPARATOR = ",";

    private final static DateTimeFormatter FIRMS_DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .toFormatter();

    private final FirmsClient firmsClient;
    private final DataLakeDao dataLakeDao;

    @Autowired
    public FirmsImportJob(FirmsClient firmsClient, DataLakeDao dataLakeDao) {
        this.firmsClient = firmsClient;
        this.dataLakeDao = dataLakeDao;
    }

    @Override
    @Timed(value = "job.firms.firmsImportJob", longTask = true)
    public void run() {
        LOG.info("Firms import job has started");

        createDataLakes(MODIS_PROVIDER, firmsClient.getModisData());
        createDataLakes(NOAA_PROVIDER, firmsClient.getNoaa20VirsData());
        createDataLakes(SUOMI_PROVIDER, firmsClient.getSuomiNppVirsData());

        LOG.info("Firms import job has finished");
    }

    private void createDataLakes(String provider, String data) {
        String[] csvRows = data.split("\n");
        String csvHeader = csvRows[0];

        for (int i = 1; i < csvRows.length; i++) {
            String csvRow = csvRows[i];
            String externalId = DigestUtils.md5Hex(csvRow);

            boolean doesRowNotExistInDataLake = dataLakeDao.getDataLakesByExternalId(externalId).isEmpty();
            if (doesRowNotExistInDataLake) {
                DataLake dataLake = new DataLake();

                dataLake.setObservationId(UUID.randomUUID());
                dataLake.setExternalId(externalId);
                dataLake.setData(csvHeader + "\n" + csvRow);
                dataLake.setLoadedAt(DateTimeUtil.uniqueOffsetDateTime());
                dataLake.setProvider(provider);
                Map<String, String> csvData = parseRow(csvHeader, csvRow);
                dataLake.setUpdatedAt(extractUpdatedAtValue(csvData));

                dataLakeDao.storeEventData(dataLake);
            }
        }
    }

    private OffsetDateTime extractUpdatedAtValue(Map<String, String> collect) {
        return OffsetDateTime.of(
                LocalDate.parse(collect.get("acq_date")),
                LocalTime.parse(collect.get("acq_time"), FIRMS_DATE_TIME_FORMATTER),
                ZoneOffset.UTC);
    }

    private Map<String, String> parseRow(String csvHeader, String csvRow) {
        String[] csvRows = csvRow.split(CSV_SEPARATOR);
        String[] csvHeaders = csvHeader.split(CSV_SEPARATOR);

        return IntStream.range(0, csvHeaders.length).boxed()
                .collect(Collectors.toMap(i -> csvHeaders[i], i -> csvRows[i]));
    }

}
