--liquibase formatted sql

--changeset event-api-migrations:v1.22.0/create-feed-data-feed-id-updated-at-index.sql runOnChange:true

create index feed_data_feed_id_updated_at_idx on feed_data using btree (feed_id, updated_at)
    where is_latest_version and enriched;
