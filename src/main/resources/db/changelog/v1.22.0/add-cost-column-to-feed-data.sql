--liquibase formatted sql

--changeset event-api-migrations:v1.22.0/add-cost-column-to-feed-data.sql runOnChange:false

alter table feed_data
    add column if not exists cost jsonb;
