package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.entity.PgSetting;
import io.kontur.eventapi.entity.PgStatTable;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;

import java.util.Map;

@Mapper
public interface GeneralMapper {

    @MapKey("name")
    Map<String, PgSetting> getPgSettings();

    @MapKey("tableName")
    Map<String, PgStatTable> getPgStatTables();
}
