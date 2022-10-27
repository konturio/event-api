--liquibase formatted sql

--changeset event-api-migrations:v1.14/rework-feed-data-indexes.sql runOnChange:true

drop index feed_data_enrichment_skipped_idx;

create index feed_data_enrichment_skipped_idx on feed_data using btree (enrichment_skipped);

drop index feed_data_feed_id_idx;

drop index feed_data_feed_id_severity_collected_geometry_idx;

create index feed_data_severity_collected_geometry_idx on feed_data using gist (collected_geometry, severity) WHERE (is_latest_version AND enriched);

drop index feed_data_updated_at_feed_id_is_latest_version_enriched_idx;

create index feed_data_updated_at_latest_version_enriched_idx on feed_data using btree (updated_at) WHERE (is_latest_version AND enriched);

drop index feed_data_updated_at_feed_id_is_not_enriched_idx;

create index feed_data_updated_at_is_not_enriched_idx on feed_data using btree (enrichment_attempts NULLS FIRST, updated_at) WHERE NOT enriched;