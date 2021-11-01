--liquibase formatted sql

--changeset event-api-migrations:v0.8/add-location-column-to-feed-data.sql runOnChange:false

alter table feed_data add column if not exists location text;


