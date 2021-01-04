package io.kontur.eventapi.emdat.jobs;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.emdat.dto.EmDatPublicFile;
import io.kontur.eventapi.emdat.service.EmDatImportService;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.util.DateTimeUtil;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static io.kontur.eventapi.util.CsvUtil.parseRow;

@Component
public class EmDatImportJob implements Runnable {

    public static final String EM_DAT_PROVIDER = "em-dat";

    private static final Logger LOG = LoggerFactory.getLogger(EmDatImportJob.class);
    private static final String CSV_ID_HEADER = "Dis No";
    private static final DataFormatter DATA_FORMATTER = new DataFormatter(Locale.ENGLISH);

    private final EmDatImportService importService;
    private final DataLakeDao dataLakeDao;

    public EmDatImportJob(EmDatImportService importService, DataLakeDao dataLakeDao) {
        this.importService = importService;
        this.dataLakeDao = dataLakeDao;
    }

    @Override
    @Counted(value = "job.emdat_import.counter")
    @Timed(value = "job.emdat_import.in_progress_timer", longTask = true)
    public void run() {
        LOG.info("EM-DAT import job has started");
        String token = importService.obtainAuthToken();
        EmDatPublicFile emDatPublicFile = importService.obtainFileStatistic(token);

        try (final InputStream stream = importService.obtainFile(emDatPublicFile.getName(), token);
             final Workbook wb = WorkbookFactory.create(stream)) {
            Sheet sheet = getDataSheet(wb);
            int headerRowNum = findHeaderRow(sheet);
            String csvHeader = convertRowIntoCsv(sheet.getRow(headerRowNum));
            for (int dataRowNum = headerRowNum + 1; dataRowNum <= sheet.getLastRowNum(); dataRowNum++) {
                String csvData = convertRowIntoCsv(sheet.getRow(dataRowNum));
                try {
                    Map<String, String> row = parseRow(csvHeader, csvData);
                    String externalId = row.get(CSV_ID_HEADER);
                    boolean doesRowNotExistInDataLake = dataLakeDao.getDataLakesByExternalId(externalId).isEmpty();
                    if (doesRowNotExistInDataLake) {
                        convertAndStore(row, csvHeader, csvData);
                    }
                } catch (Exception e) {
                    LOG.error("Can't create EM-DAT row: {}", csvData, e);
                }
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        LOG.info("EM-DAT import job has finished");
    }

    private Sheet getDataSheet(Workbook wb) {
        Sheet sheet = wb.getSheet("emdat data");
        if (sheet == null) {
            throw new IllegalArgumentException("Illegal EM-DAT xlsx format. Could not find 'emdat data' sheet");
        }
        return sheet;
    }

    private int findHeaderRow(Sheet sheet) {
        for (int i = 0; i < sheet.getLastRowNum(); i++) {
            if (CSV_ID_HEADER.equals(sheet.getRow(i).getCell(0).getStringCellValue())) {
                return i;
            }
        }
        throw new IllegalArgumentException("Illegal EM-DAT xlsx format. Could not find table header");
    }

    private String convertRowIntoCsv(Row row) {
        StringBuilder builder = new StringBuilder();
        for (int cn = 0; cn < row.getLastCellNum(); cn++) {
            Cell cell = row.getCell(cn, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            String cellValue = DATA_FORMATTER.formatCellValue(cell);
            if (cellValue != null && !cellValue.isBlank()) {
                builder.append(StringEscapeUtils.escapeCsv(cellValue));
            }
            if (cn < row.getLastCellNum() - 1) {
                builder.append(",");
            }
        }
        return builder.toString();
    }

    private void convertAndStore(Map<String, String> row, String csvHeader,
                                 String csvData) {
        DataLake dataLake = new DataLake();

        dataLake.setObservationId(UUID.randomUUID());
        dataLake.setExternalId(row.get(CSV_ID_HEADER));
        dataLake.setData(csvHeader + "\n" + csvData);
        dataLake.setLoadedAt(DateTimeUtil.uniqueOffsetDateTime());
        dataLake.setProvider(EM_DAT_PROVIDER);
        dataLake.setUpdatedAt(getExtractStartedAtValue(row, csvData));

        dataLakeDao.storeEventData(dataLake);
    }

    private OffsetDateTime getExtractStartedAtValue(Map<String, String> row, String csvData) {
        int startYear = Integer.parseInt(row.get("Start Year"));
        int startMonth = Integer.parseInt(getOrDefault(row.get("Start Month"), "1"));
        int startDay = Integer.parseInt(getOrDefault(row.get("Start Day"), "1"));
        LocalDateTime ldt;
        try {
            ldt = LocalDateTime.of(startYear, startMonth, startDay, 0, 0, 0);
        } catch (DateTimeException e) {
            LOG.warn("'{}' for {}", e.getMessage(), csvData);
            ldt = LocalDateTime.of(startYear, startMonth, 1, 0, 0, 0);
        }
        return OffsetDateTime.of(ldt, ZoneOffset.UTC);
    }

    private String getOrDefault(String value, String def) {
        if (value == null || value.isBlank()) {
            return def;
        }
        return value;
    }
}
