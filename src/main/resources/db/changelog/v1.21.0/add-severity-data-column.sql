--liquibase formatted sql

--changeset event-api-migrations:v1.19.0/add-severity-data-column.sql runOnChange:true

alter table normalized_observations
add column severity_data jsonb default '{}'::jsonb;

alter table feed_data
add column severity_data jsonb default '{}'::jsonb;