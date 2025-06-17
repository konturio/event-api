package io.kontur.eventapi.tornadojapanma.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.tornadojapanma.dto.ParsedCase;
import io.kontur.eventapi.util.DateTimeUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class TornadoJapanMaImportService {

    public final static String TORNADO_JAPAN_MA_PROVIDER = "tornado.japan-ma";

    private final static Logger LOG = LoggerFactory.getLogger(TornadoJapanMaImportService.class);
    private final static ObjectMapper objectMapper = new ObjectMapper();
    private final DataLakeDao dataLakeDao;

    public TornadoJapanMaImportService(DataLakeDao dataLakeDao) {
        this.dataLakeDao = dataLakeDao;
    }

    public String downloadCsv(String url) throws Exception {
        return Jsoup.connect(url).ignoreContentType(true).execute().body();
    }

    public void storeDataLakes(Set<ParsedCase> parsedCases, OffsetDateTime updatedAt) {
        List<DataLake> dataLakes = new ArrayList<>();
        for (ParsedCase parsedCase : parsedCases) {
            try {
                String data = objectMapper.writeValueAsString(parsedCase);
                String externalId = DigestUtils.md5Hex(data);
                if (dataLakeDao.getLatestDataLakeByExternalIdAndProvider(externalId, TORNADO_JAPAN_MA_PROVIDER).isEmpty()) {
                    DataLake dataLake = new DataLake(UUID.randomUUID(), externalId, updatedAt, DateTimeUtil.uniqueOffsetDateTime());
                    dataLake.setProvider(TORNADO_JAPAN_MA_PROVIDER);
                    dataLake.setData(data);
                    dataLakes.add(dataLake);
                }
            } catch (Exception e) {
                LOG.warn(e.getMessage());
            }
        }
        dataLakeDao.storeDataLakes(dataLakes);
    }

    public void storeCsvData(String csvData, OffsetDateTime updatedAt) {
        String[] rows = csvData.split("\r?\n");
        if (rows.length == 0) {
            return;
        }
        String header = rows[0];
        List<DataLake> dataLakes = new ArrayList<>();
        for (int i = 1; i < rows.length; i++) {
            String row = rows[i];
            if (StringUtils.isBlank(row)) {
                continue;
            }
            try {
                String externalId = DigestUtils.md5Hex(row);
                if (dataLakeDao.getLatestDataLakeByExternalIdAndProvider(externalId, TORNADO_JAPAN_MA_PROVIDER).isEmpty()) {
                    DataLake dataLake = new DataLake(UUID.randomUUID(), externalId, updatedAt, DateTimeUtil.uniqueOffsetDateTime());
                    dataLake.setProvider(TORNADO_JAPAN_MA_PROVIDER);
                    dataLake.setData(header + "\n" + row);
                    dataLakes.add(dataLake);
                }
            } catch (Exception e) {
                LOG.warn(e.getMessage());
            }
        }
        dataLakeDao.storeDataLakes(dataLakes);
    }

    public OffsetDateTime convertDate(String dateString) {
        LocalDate localDate = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyy.M.dd"));
        return OffsetDateTime.of(localDate, LocalTime.MIN, ZoneOffset.UTC);
    }
}
