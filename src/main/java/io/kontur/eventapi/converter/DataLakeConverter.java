package io.kontur.eventapi.converter;

import io.kontur.eventapi.dto.ParsedItem;
import io.kontur.eventapi.entity.DataLake;

public interface DataLakeConverter {
    DataLake convertEvent(ParsedItem event, String provider);
}
