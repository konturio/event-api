package io.kontur.eventapi.converter;

import io.kontur.eventapi.dto.FeedDataDto;
import io.kontur.eventapi.resource.dto.EventDto;
import org.springframework.beans.BeanUtils;

public class EventDtoConverter {

    public static EventDto convert(FeedDataDto dataDto) {
        EventDto eventDto = new EventDto();
        BeanUtils.copyProperties(dataDto, eventDto);
        return eventDto;
    }

}
