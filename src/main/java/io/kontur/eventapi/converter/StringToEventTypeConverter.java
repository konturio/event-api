package io.kontur.eventapi.converter;

import io.kontur.eventapi.dto.EventType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.StringUtils;

public class StringToEventTypeConverter implements Converter<String, EventType> {

    @Override
    public EventType convert(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        return EventType.fromString(value);
    }
}
