package io.kontur.eventapi.dao;

import io.kontur.eventapi.dao.mapper.CombinedEventsMapper;
import io.kontur.eventapi.dto.CombinedAreaDto;
import io.kontur.eventapi.dto.CombinedEpisodeDto;
import io.kontur.eventapi.dto.CombinedEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class CombinedEventsDao {

    private static final Logger LOG = LoggerFactory.getLogger(CombinedEventsDao.class);
    private final CombinedEventsMapper mapper;

    public CombinedEventsDao(CombinedEventsMapper mapper) {
        this.mapper = mapper;
    }

    public Optional<CombinedEventDto> getEventForExternalId(String externalId) {
        List<CombinedEventDto> eventForExternalId = mapper.getEventForExternalId(externalId);

        if (eventForExternalId.isEmpty()) {
            return Optional.empty();
        }
        if (eventForExternalId.size() > 1) {
            LOG.warn("Several combined events have been found for one external id. External id = '{}'", externalId);
        }
        return Optional.of(eventForExternalId.get(0));
    }

    @Transactional
    public void saveEvent(CombinedEventDto event) {
        if (event.getId() == null) {
            mapper.insertEvent(event);
        } else {
            // TODO update events?
        }

        event.getEpisodes().forEach(episode -> {
            episode.setEventId(event.getId());
            saveEpisode(episode);

            episode.getAreas().forEach(area -> {
                area.setEpisodeId(episode.getId());
                saveArea(area);
            });
        });
    }

    private void saveEpisode(CombinedEpisodeDto episode) {
        if (episode.getId() == null) {
            mapper.insertEpisode(episode);
        } else {
            // TODO update episodes?
        }
    }

    private void saveArea(CombinedAreaDto area) {
        if (area.getId() == null) {
            mapper.insertArea(area);
        } else {
            // TODO update areas?
        }
    }

}
