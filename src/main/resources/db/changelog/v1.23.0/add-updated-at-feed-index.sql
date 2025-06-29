--liquibase formatted sql

--changeset event-api-migrations:v1.23.0/add-updated-at-feed-index.sql runOnChange:false
create index if not exists feed_data_updated_at_feed_id_is_latest_version_enriched_idx
    on feed_data using btree (updated_at, feed_id)
    where is_latest_version and enriched;
