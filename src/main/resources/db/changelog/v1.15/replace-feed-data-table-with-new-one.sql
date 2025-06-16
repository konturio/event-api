--liquibase formatted sql

--changeset event-api-migrations:v1.15/replace-feed-data-table-with-new-one.sql runOnChange:true

--BEGIN;
alter table public.feed_data
    RENAME TO feed_data__old;
alter table public.feed_data_upd
    RENAME TO feed_data;
--COMMIT;
