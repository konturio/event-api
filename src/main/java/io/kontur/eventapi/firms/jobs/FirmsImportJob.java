package io.kontur.eventapi.firms.jobs;

import feign.FeignException;
import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.firms.client.FirmsClient;
import io.kontur.eventapi.firms.dto.ParsedDataLakeItem;
import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.util.DateTimeUtil;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.stream.Collectors;

import static io.kontur.eventapi.util.CsvUtil.parseDataLakeRow;

@Component
public abstract class FirmsImportJob extends AbstractJob {
    private static final Logger LOG = LoggerFactory.getLogger(FirmsImportJob.class);

    private static final int STEP = 32700;

    private final static DateTimeFormatter FIRMS_DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .toFormatter();

    protected final FirmsClient firmsClient;
    protected final DataLakeDao dataLakeDao;

    @Autowired
    public FirmsImportJob(FirmsClient firmsClient, DataLakeDao dataLakeDao, MeterRegistry meterRegistry) {
        super(meterRegistry);
        this.firmsClient = firmsClient;
        this.dataLakeDao = dataLakeDao;
    }

    protected abstract List<DataLake> loadData();

    @Override
    public void execute() {
        try {
            List<DataLake> dataLakes = loadData();
            List<DataLake> sortedDataLakes = dataLakes.stream()
                    .sorted(Comparator.comparing(DataLake::getUpdatedAt, Comparator.nullsFirst(Comparator.naturalOrder())))
                    .peek(dataLake -> dataLake.setLoadedAt(DateTimeUtil.uniqueOffsetDateTime()))
                    .collect(Collectors.toList());

            dataLakeDao.storeDataLakes(sortedDataLakes);
        } catch (FeignException e) {
            LOG.warn("Failed to load FIRMS data: " + e.getMessage(), e);
        }
    }

    @Override
    public String getName() {
        return "firmsImport";
    }

    protected List<DataLake> createDataLakes(String provider, String data) {
        List<DataLake> dataLakes = new ArrayList<>();
        String[] csvRows = data.split("\r?\n");
        String csvHeader = csvRows[0];

        Set<String> ids = Arrays.stream(csvRows).skip(1L).map(DigestUtils::md5Hex).collect(Collectors.toSet());
        if (!CollectionUtils.isEmpty(ids)) {
            LOG.info("Processing {} records for {}", ids.size(), provider);
            List<String> existsDataLakeIds = new ArrayList<>();
            long count = 0;
            while (count * STEP < ids.size()) {
                List<DataLake> existsDataLakesParts = dataLakeDao.getDataLakesByExternalIds(
                        ids.stream().skip(count * STEP).limit(STEP).collect(Collectors.toSet()));
                existsDataLakeIds.addAll(
                        existsDataLakesParts.stream().map(DataLake::getExternalId).collect(Collectors.toSet()));
                count++;
            }
            for (int i = 1; i < csvRows.length; i++) {
                String csvRow = csvRows[i];
                String externalId = DigestUtils.md5Hex(csvRow);
                if (!existsDataLakeIds.contains(externalId)) {
                    existsDataLakeIds.add(externalId);
                    ParsedDataLakeItem csvData = parseDataLakeRow(provider, csvHeader, csvRow);
                    if (csvData != null && isValidData(csvData)){
                        DataLake dataLake = new DataLake();

                        dataLake.setObservationId(UUID.randomUUID());
                        dataLake.setExternalId(externalId);
                        dataLake.setData(csvHeader + "\n" + csvRow);
                        dataLake.setProvider(provider);
                        dataLake.setUpdatedAt(extractUpdatedAtValue(csvData));

                        dataLakes.add(dataLake);
                    }
                }
            }
        }
        return dataLakes;
    }

    protected OffsetDateTime extractUpdatedAtValue(ParsedDataLakeItem collect) {
        return OffsetDateTime.of(
                LocalDate.parse(collect.getAcqDate()),
                LocalTime.parse(collect.getAcqTime(), FIRMS_DATE_TIME_FORMATTER),
                ZoneOffset.UTC);
    }

    protected boolean isValidData(ParsedDataLakeItem csvData) {
        return StringUtils.isNotBlank(csvData.getAcqDate()) && StringUtils.isNotBlank(csvData.getAcqTime());
    }
}
