package io.kontur.eventapi.typehandler;

import io.kontur.eventapi.entity.Severity;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SeverityArrayTypeHandler extends BaseTypeHandler<List<Severity>> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<Severity> severities, JdbcType jdbcType) throws SQLException {
        Array array = ps.getConnection().createArrayOf("text", severities.toArray());
        ps.setArray(i, array);
    }

    @Override
    public List<Severity> getNullableResult(ResultSet rs, String s) throws SQLException {
        return toList(rs.getArray(s));
    }

    @Override
    public List<Severity> getNullableResult(ResultSet rs, int i) throws SQLException {
        return toList(rs.getArray(i));
    }

    @Override
    public List<Severity> getNullableResult(CallableStatement cs, int i) throws SQLException {
        return toList(cs.getArray(i));
    }

    private List<Severity> toList(Array pgArray) throws SQLException {
        if (pgArray == null) return Collections.emptyList();

        String[] strings = (String[]) pgArray.getArray();
        Severity[] severities = Arrays.stream(strings).map(Severity::valueOf).toArray(Severity[]::new);
        return containsOnlyNulls(severities) ? Collections.emptyList() : new ArrayList<>(List.of(severities));
    }

    private boolean containsOnlyNulls(Severity[] severities) {
        for (Severity s : severities) {
            if (s != null) {
                return false;
            }
        }
        return true;
    }
}
