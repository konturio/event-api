package io.kontur.eventapi.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.*;
import java.util.*;

public class UUIDArrayTypeHandler extends BaseTypeHandler<List<UUID>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<UUID> parameter,
                                    JdbcType jdbcType) throws SQLException {
        Array array = ps.getConnection().createArrayOf("uuid", parameter.toArray());
        ps.setArray(i, array);
    }

    @Override
    public List<UUID> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toList(rs.getArray(columnName));
    }

    @Override
    public List<UUID> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toList(rs.getArray(columnIndex));
    }

    @Override
    public List<UUID> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toList(cs.getArray(columnIndex));
    }

    private List<UUID> toList(Array pgArray) throws SQLException {
        if (pgArray == null) return Collections.emptyList();

        UUID[] uuids = (UUID[]) pgArray.getArray();
        return containsOnlyNulls(uuids) ? Collections.emptyList() : new ArrayList<>(List.of(uuids));
    }

    private boolean containsOnlyNulls(UUID[] uuids) {
        for (UUID uuid : uuids) {
            if (uuid != null) {
                return false;
            }
        }
        return true;
    }

}
