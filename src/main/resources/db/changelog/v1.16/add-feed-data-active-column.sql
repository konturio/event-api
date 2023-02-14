--liquibase formatted sql

--changeset event-api-migrations:v1.16/add-feed-data-active-column.sql runOnChange:true

alter table feed_data add column if not exists active boolean;

alter table feed_data
    add column if not exists auto_expire boolean default false;