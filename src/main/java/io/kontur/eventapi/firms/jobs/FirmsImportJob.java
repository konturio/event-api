package io.kontur.eventapi.firms.jobs;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.firms.client.FirmsClient;
import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.util.DateTimeUtil;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.codec.digest.DigestUtils;
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

import static io.kontur.eventapi.firms.FirmsUtil.*;
import static io.kontur.eventapi.util.CsvUtil.parseRow;

@Component
public class FirmsImportJob extends AbstractJob {
    private final static DateTimeFormatter FIRMS_DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .toFormatter();

    private final FirmsClient firmsClient;
    private final DataLakeDao dataLakeDao;

    @Autowired
    public FirmsImportJob(FirmsClient firmsClient, DataLakeDao dataLakeDao, MeterRegistry meterRegistry) {
        super(meterRegistry);
        this.firmsClient = firmsClient;
        this.dataLakeDao = dataLakeDao;
    }

    @Override
    public void execute() {
        List<DataLake> dataLakesFromModis = createDataLakes(MODIS_PROVIDER, firmsClient.getModisData());
        List<DataLake> dataLakesFromNoaa= createDataLakes(NOAA_PROVIDER, firmsClient.getNoaa20VirsData());
        List<DataLake> dataLakesFromSuomi = createDataLakes(SUOMI_PROVIDER, firmsClient.getSuomiNppVirsData());

        List<DataLake> allDataLakes = new ArrayList<>();
        allDataLakes.addAll(dataLakesFromModis);
        allDataLakes.addAll(dataLakesFromNoaa);
        allDataLakes.addAll(dataLakesFromSuomi);

        List<DataLake> sortedDataLakes = allDataLakes.stream()
                .sorted(Comparator.comparing(DataLake::getUpdatedAt, Comparator.nullsFirst(Comparator.naturalOrder())))
                .peek(dataLake -> dataLake.setLoadedAt(DateTimeUtil.uniqueOffsetDateTime()))
                .collect(Collectors.toList());

        dataLakeDao.storeDataLakes(sortedDataLakes);
    }

    @Override
    public String getName() {
        return "firmsImport";
    }

    private List<DataLake> createDataLakes(String provider, String data) {
        List<DataLake> dataLakes = new ArrayList<>();
        String[] csvRows = data.split("\r?\n");
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
                dataLake.setProvider(provider);
                Map<String, String> csvData = parseRow(csvHeader, csvRow);
                dataLake.setUpdatedAt(extractUpdatedAtValue(csvData));

                dataLakes.add(dataLake);
            }
        }

        return dataLakes;
    }

    private OffsetDateTime extractUpdatedAtValue(Map<String, String> collect) {
        return OffsetDateTime.of(
                LocalDate.parse(collect.get("acq_date")),
                LocalTime.parse(collect.get("acq_time"), FIRMS_DATE_TIME_FORMATTER),
                ZoneOffset.UTC);
    }
}
