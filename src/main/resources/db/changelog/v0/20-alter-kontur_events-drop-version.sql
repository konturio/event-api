--liquibase formatted sql

--changeset event-api-migrations:20-alter-kontur_events-drop-version.sql runOnChange:false

alter table kontur_events drop constraint kontur_events_event_id_version_observation_id_key;
alter table kontur_events drop column version;
alter table kontur_events add constraint kontur_events_event_id_observation_id_key unique (event_id, observation_id);

drop index feed_data_updated_at_unique;
create unique index feed_data_updated_at_version_unique on feed_data (updated_at, version);
create index feed_data_observations on feed_data using gin(observations);
