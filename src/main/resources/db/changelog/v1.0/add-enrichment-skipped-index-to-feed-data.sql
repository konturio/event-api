--liquibase formatted sql

--changeset event-api-migrations:v1.0/add-enrichment-skipped-index-to-feed-data.sql runOnChange:false

create index feed_data_enrichment_skipped_idx
    on feed_data (feed_id)
    where (enrichment_skipped);