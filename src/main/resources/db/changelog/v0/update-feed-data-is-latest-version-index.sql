--liquibase formatted sql

--changeset event-api-migrations:update-feed-data-is-latest-version-index.sql runOnChange:false

drop index if exists feed_data_updated_at_feed_id_is_latest_version_idx;

create index if not exists feed_data_updated_at_feed_id_is_latest_version_enriched_idx on public.feed_data using btree (updated_at, feed_id) where is_latest_version and enriched;
