package io.kontur.eventapi.typehandler;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static io.kontur.eventapi.util.JsonUtil.readJson;
import static io.kontur.eventapi.util.JsonUtil.writeJson;

public class FeatureCollectionTypeHandler extends BaseTypeHandler<FeatureCollection> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, FeatureCollection parameter, JdbcType jdbcType) throws SQLException {
        ps.setObject(i, writeJson(parameter));
    }

    @Override
    public FeatureCollection getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        if (StringUtils.isEmpty(value)) {
            return new FeatureCollection(new Feature[] {});
        }
        return readJson(value, new TypeReference<>() {});
    }

    @Override
    public FeatureCollection getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        if (StringUtils.isEmpty(value)) {
            return new FeatureCollection(new Feature[] {});
        }
        return readJson(value, new TypeReference<>() {});
    }

    @Override
    public FeatureCollection getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        if (StringUtils.isEmpty(value)) {
            return new FeatureCollection(new Feature[] {});
        }
        return readJson(value, new TypeReference<>() {});
    }
}
