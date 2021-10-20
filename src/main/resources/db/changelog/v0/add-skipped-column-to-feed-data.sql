--liquibase formatted sql

--changeset event-api-migrations:v0/add-skipped-column-to-feed-data.sql runOnChange:false

alter table feed_data add column enrichment_skipped bool default false;
