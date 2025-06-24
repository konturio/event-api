--liquibase formatted sql

--changeset event-api-migrations:v1.23.0/add-merge-done-column.sql runOnChange:false
alter table feed_data add column if not exists merge_done boolean default true;
