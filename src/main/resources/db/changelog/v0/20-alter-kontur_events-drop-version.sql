--liquibase formatted sql

--changeset event-api-migrations:20-alter-kontur_events-drop-version.sql runOnChange:false

ALTER TABLE kontur_events DROP CONSTRAINT kontur_events_event_id_version_observation_id_key;
ALTER TABLE kontur_events DROP COLUMN version;
ALTER TABLE kontur_events ADD CONSTRAINT kontur_events_event_id_observation_id_key UNIQUE (event_id, observation_id);

DROP INDEX feed_data_updated_at_unique;
CREATE UNIQUE INDEX feed_data_updated_at_version_unique ON feed_data (updated_at, version);
CREATE INDEX feed_data_observations ON feed_data USING GIN(observations);
