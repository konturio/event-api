--liquibase formatted sql

--changeset event-api-migrations:v0.8/add-url-column-to-feed-data.sql runOnChange:false

alter table feed_data add column if not exists urls text[] default '{}'::text[];


