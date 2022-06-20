package io.kontur.eventapi.cap.converter;

import io.kontur.eventapi.cap.dto.CapParsedItem;
import io.kontur.eventapi.entity.DataLake;

public interface CapDataLakeConverter {
    DataLake convertEvent(CapParsedItem event, String provider);
}
