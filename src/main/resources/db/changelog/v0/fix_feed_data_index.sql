--liquibase formatted sql

--changeset event-api-migrations:v0/fix_feed_data_index.sql runOnChange:false
drop index feed_data_observations;
drop index feed_data_updated_at_version_unique;
create unique index feed_data_event_id_updated_at_version_unique on feed_data (feed_id, event_id, updated_at, version);
