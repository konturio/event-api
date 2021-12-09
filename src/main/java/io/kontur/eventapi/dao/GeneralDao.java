package io.kontur.eventapi.dao;

import io.kontur.eventapi.dao.mapper.GeneralMapper;
import io.kontur.eventapi.entity.PgSetting;
import io.kontur.eventapi.entity.PgStatTable;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GeneralDao {
    private final GeneralMapper mapper;

    public GeneralDao(GeneralMapper mapper) {
        this.mapper = mapper;
    }

    public Map<String, PgSetting> getPgSettings() {
        return mapper.getPgSettings();
    }

    public Map<String, PgStatTable> getPgStatTables() {
        return mapper.getPgStatTables();
    }
}
