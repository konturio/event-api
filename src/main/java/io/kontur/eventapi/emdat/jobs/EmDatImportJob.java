package io.kontur.eventapi.emdat.jobs;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.emdat.dto.EmDatPublicFile;
import io.kontur.eventapi.emdat.service.EmDatImportService;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.util.DateTimeUtil;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.text.StringEscapeUtils;
import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static io.kontur.eventapi.util.CsvUtil.parseRow;
import static java.util.Comparator.comparing;

@Component
public class EmDatImportJob extends AbstractJob {

    public static final String EM_DAT_PROVIDER = "em-dat";

    private static final Logger LOG = LoggerFactory.getLogger(EmDatImportJob.class);
    private static final String CSV_ID_HEADER = "Dis No";
    private static final String FILE_CREATION_CELL = "File creation:";
    private static final DateTimeFormatter FILE_CREATION_FORMATTER = DateTimeFormatter
            .ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
    private final EmDatImportService importService;
    private final DataLakeDao dataLakeDao;

    public EmDatImportJob(MeterRegistry registry, EmDatImportService importService, DataLakeDao dataLakeDao) {
        super(registry);
        this.importService = importService;
        this.dataLakeDao = dataLakeDao;
    }

    @Override
    public void execute() throws Exception {
        String token = importService.obtainAuthToken();
        EmDatPublicFile emDatPublicFile = importService.obtainFileStatistic(token);

        try (final InputStream stream = importService.obtainFile(emDatPublicFile.getName(), token);
             final ReadableWorkbook wb = new ReadableWorkbook(stream)) {
            Sheet sheet = getDataSheet(wb);
            parseAndSaveContent(sheet);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    @Override
    public String getName() {
        return "emdatImport";
    }

    private Sheet getDataSheet(ReadableWorkbook wb) {
        Optional<Sheet> sheet = wb.getSheets()
                .filter(s -> "emdat data".equals(s.getName()))
                .findFirst();

        if (sheet.isEmpty()) {
            throw new IllegalArgumentException("Illegal EM-DAT xlsx format. Could not find 'emdat data' sheet");
        }
        return sheet.get();
    }

    private void parseAndSaveContent(Sheet sheet) throws IOException {
        List<Row> rows = sheet.read();
        OffsetDateTime fileCreationDate = getFileCreationDate(rows);
        int headerRowNum = findHeaderRow(rows);
        String csvHeader = convertRowIntoCsv(rows.get(headerRowNum));
        for (int dataRowNum = headerRowNum + 1; dataRowNum < rows.size(); dataRowNum++) {
            String csvData = convertRowIntoCsv(rows.get(dataRowNum));
            try {
                Map<String, String> row = parseRow(csvHeader, csvData);
                String externalId = row.get(CSV_ID_HEADER);
                String data = csvHeader + "\n" + csvData;
                if (isNewEvent(externalId, data)) {
                    save(row, data, fileCreationDate);
                }
            } catch (Exception e) {
                LOG.error("Can't create EM-DAT row: {}", csvData, e);
            }
        }
    }

    private boolean isNewEvent(String externalId, String data) {
        Optional<DataLake> dataLake = dataLakeDao.getDataLakesByExternalId(externalId)
                .stream()
                .max(comparing(DataLake::getLoadedAt));
        return dataLake.isEmpty() || !data.equals(dataLake.get().getData());
    }

    private int findHeaderRow(List<Row> rows) {
        for (int i = 0; i < rows.size(); i++) {
            Optional<String> cellValue = rows.get(i).getCellAsString(0);
            if (cellValue.isPresent() && CSV_ID_HEADER.equals(cellValue.get())) {
                return i;
            }
        }
        throw new IllegalArgumentException("Illegal EM-DAT xlsx format. Could not find table header");
    }

    private OffsetDateTime getFileCreationDate(List<Row> rows) {
        for (Row row : rows) {
            Cell cellName = row.getCell(0);
            if (cellName != null && FILE_CREATION_CELL.equals(cellName.getText())) {
                String cellValue = row.getCell(1).getText();
                return ZonedDateTime
                        .parse(cellValue, FILE_CREATION_FORMATTER)
                        .toOffsetDateTime();
            }
        }
        throw new IllegalArgumentException(
                String.format("Illegal EM-DAT xlsx format. Could not find %s cell", FILE_CREATION_CELL));
    }

    private String convertRowIntoCsv(Row row) {
        StringBuilder builder = new StringBuilder();
        for (int cn = 0; cn < row.getCellCount(); cn++) {
            String cellValue = null;
            if (row.getCell(cn) != null) {
                cellValue = row.getCell(cn).getText();
            }
            if (cellValue != null && !cellValue.isBlank()) {
                builder.append(StringEscapeUtils.escapeCsv(cellValue));
            }
            if (cn < row.getCellCount() - 1) {
                builder.append(",");
            }
        }
        return builder.toString();
    }

    private void save(Map<String, String> row, String data, OffsetDateTime fileCreationDate) {
        DataLake dataLake = new DataLake();

        dataLake.setObservationId(UUID.randomUUID());
        dataLake.setProvider(EM_DAT_PROVIDER);
        dataLake.setExternalId(row.get(CSV_ID_HEADER));
        dataLake.setData(data);
        dataLake.setLoadedAt(DateTimeUtil.uniqueOffsetDateTime());
        dataLake.setUpdatedAt(fileCreationDate);

        dataLakeDao.storeEventData(dataLake);
    }
}
