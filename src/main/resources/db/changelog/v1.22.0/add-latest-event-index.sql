--liquibase formatted sql

--changeset event-api-migrations:v1.22.0/add-latest-event-index.sql runOnChange:false
create index if not exists feed_data_event_feed_latest_idx
    on feed_data using btree (event_id, feed_id)
    where is_latest_version;
