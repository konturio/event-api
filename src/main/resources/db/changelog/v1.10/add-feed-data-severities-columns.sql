--liquibase formatted sql

--changeset event-api-migrations:v1.10/add-feed-data-severities-columns.sql runOnChange:false

alter table feed_data
    add if not exists latest_severity text;

alter table feed_data
    add if not exists severities text[];