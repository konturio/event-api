--liquibase formatted sql

--changeset event-api-migrations:v1.16/add-feed-data-loss-column.sql runOnChange:true

alter table feed_data
add column loss jsonb default '{}';