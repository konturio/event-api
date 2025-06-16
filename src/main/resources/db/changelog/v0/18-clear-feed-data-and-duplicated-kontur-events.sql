--liquibase formatted sql

--changeset event-api-migrations:14-clear-feed-data-kontur-events-normalized-observations runOnChange:false

delete from feed_data;

WITH latest_events as (select event_id, observation_id, max(version) as max_version
                          from kontur_events
                          group by event_id, observation_id)
delete from kontur_events e using latest_events
where e.event_id = latest_events.event_id
     and e.observation_id = latest_events.observation_id
     and e.version != latest_events.max_version;