package io.kontur.eventapi.typehandler;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BigDecimalJsonbTypeHandler extends BaseTypeHandler<BigDecimal> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, BigDecimal parameter,
                                    JdbcType jdbcType) throws SQLException {
        ps.setObject(i, parameter);
    }

    @Override
    public BigDecimal getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return StringUtils.isBlank(value) ? null : new BigDecimal(value);
    }

    @Override
    public BigDecimal getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return StringUtils.isBlank(value) ? null : new BigDecimal(value);
    }

    @Override
    public BigDecimal getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return StringUtils.isBlank(value) ? null : new BigDecimal(value);
    }
}
