--liquibase formatted sql

--changeset event-api-migrations:v1.13/delete-feeds.sql runOnChange:false

delete from feed_data where feed_id != (select feed_id from feeds where alias = 'kontur-public');

delete from feed_event_status where feed_id != (select feed_id from feeds where alias = 'kontur-public');

delete from kontur_events where provider not in ('firms.modis-c6', 'firms.suomi-npp-viirs-c2', 'firms.noaa-20-viirs-c2', 'kontur.events');

delete from normalized_observations where provider not in ('firms.modis-c6', 'firms.suomi-npp-viirs-c2', 'firms.noaa-20-viirs-c2', 'kontur.events');