--liquibase formatted sql

--changeset event-api-migrations:v1.1/fill-enriched-at-for-skipped-events.sql runOnChange:false

update feed_data set enriched_at = now()
where enrichment_skipped and enriched_at is null;