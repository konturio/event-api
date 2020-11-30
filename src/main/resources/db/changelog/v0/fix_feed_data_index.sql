--liquibase formatted sql

--changeset event-api-migrations:v0/fix_feed_data_index.sql runOnChange:false
DROP INDEX feed_data_observations;
DROP INDEX feed_data_updated_at_version_unique;
CREATE UNIQUE INDEX feed_data_event_id_updated_at_version_unique ON feed_data (feed_id, event_id, updated_at, version);
