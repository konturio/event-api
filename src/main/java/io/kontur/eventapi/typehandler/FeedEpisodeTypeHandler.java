package io.kontur.eventapi.typehandler;

import com.fasterxml.jackson.core.type.TypeReference;
import io.kontur.eventapi.entity.FeedEpisode;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static io.kontur.eventapi.util.JsonUtil.readJson;
import static io.kontur.eventapi.util.JsonUtil.writeJson;

public class FeedEpisodeTypeHandler extends BaseTypeHandler<List<FeedEpisode>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<FeedEpisode> parameter,
                                    JdbcType jdbcType) throws SQLException {
        parameter.forEach(ep -> {
            if (ep.getObservations() != null && !ep.getObservations().isEmpty()) {
                Set<UUID> sorted = ep.getObservations().stream()
                        .filter(Objects::nonNull)
                        .sorted()
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                ep.setObservations(sorted);
            }
        });
        ps.setObject(i, writeJson(parameter));
    }

    @Override
    public List<FeedEpisode> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        if (StringUtils.isEmpty(value)) {
            return Collections.emptyList();
        }
        List<FeedEpisode> episodes = readJson(value, new TypeReference<>() {});
        episodes.forEach(ep -> {
            if (ep.getObservations() != null && !ep.getObservations().isEmpty()) {
                Set<UUID> sorted = ep.getObservations().stream()
                        .filter(Objects::nonNull)
                        .sorted()
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                ep.setObservations(sorted);
            }
        });
        return episodes;
    }

    @Override
    public List<FeedEpisode> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        if (StringUtils.isEmpty(value)) {
            return Collections.emptyList();
        }
        List<FeedEpisode> episodes = readJson(value, new TypeReference<>() {});
        episodes.forEach(ep -> {
            if (ep.getObservations() != null && !ep.getObservations().isEmpty()) {
                Set<UUID> sorted = ep.getObservations().stream()
                        .filter(Objects::nonNull)
                        .sorted()
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                ep.setObservations(sorted);
            }
        });
        return episodes;
    }

    @Override
    public List<FeedEpisode> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        if (StringUtils.isEmpty(value)) {
            return Collections.emptyList();
        }
        List<FeedEpisode> episodes = readJson(value, new TypeReference<>() {});
        episodes.forEach(ep -> {
            if (ep.getObservations() != null && !ep.getObservations().isEmpty()) {
                Set<UUID> sorted = ep.getObservations().stream()
                        .filter(Objects::nonNull)
                        .sorted()
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                ep.setObservations(sorted);
            }
        });
        return episodes;
    }
}
