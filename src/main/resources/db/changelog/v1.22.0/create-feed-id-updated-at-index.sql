--liquibase formatted sql

--changeset event-api-migrations:v1.22.0/create-feed-id-updated-at-index.sql runOnChange:true

create index concurrently if not exists feed_data_feed_id_updated_at_latest_version_enriched_idx
    on feed_data (feed_id, updated_at)
    where is_latest_version and enriched;
