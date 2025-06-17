--liquibase formatted sql

--changeset event-api-migrations:v1.22.0/add-index-feed-event.sql runOnChange:false

create index feed_data_feed_id_event_id_idx
    on feed_data(feed_id, event_id)
    where is_latest_version;
