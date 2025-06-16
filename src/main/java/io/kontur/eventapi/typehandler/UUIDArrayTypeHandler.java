package io.kontur.eventapi.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.*;
import java.util.*;

public class UUIDArrayTypeHandler extends BaseTypeHandler<Set<UUID>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Set<UUID> parameter,
                                    JdbcType jdbcType) throws SQLException {
        List<UUID> sorted = parameter.stream()
                .filter(Objects::nonNull)
                .sorted()
                .toList();
        Array array = ps.getConnection().createArrayOf("uuid", sorted.toArray());
        ps.setArray(i, array);
    }

    @Override
    public Set<UUID> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toSet(rs.getArray(columnName));
    }

    @Override
    public Set<UUID> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toSet(rs.getArray(columnIndex));
    }

    @Override
    public Set<UUID> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toSet(cs.getArray(columnIndex));
    }

    private Set<UUID> toSet(Array pgArray) throws SQLException {
        if (pgArray == null) return Collections.emptySet();

        UUID[] uuids = (UUID[]) pgArray.getArray();
        if (containsOnlyNulls(uuids)) {
            return Collections.emptySet();
        }
        List<UUID> sorted = Arrays.stream(uuids)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
        return new LinkedHashSet<>(sorted);
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
