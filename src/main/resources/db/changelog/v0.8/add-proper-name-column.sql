--liquibase formatted sql

--changeset event-api-migrations:v0.8/add-proper-name-column.sql runOnChange:false

alter table normalized_observations
    add column if not exists proper_name text;

alter table feed_data
    add column if not exists proper_name text;


