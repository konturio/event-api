--liquibase formatted sql

--changeset event-api-migrations:feed_data_enrichment_index runOnChange:true

drop index if exists feed_data_updated_at_feed_id_is_not_enriched_idx;
create index feed_data_updated_at_feed_id_is_not_enriched_idx on feed_data (enrichment_attempts nulls first, updated_at, feed_id) where not enriched;