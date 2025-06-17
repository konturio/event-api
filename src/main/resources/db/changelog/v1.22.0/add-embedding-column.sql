--liquibase formatted sql

--changeset event-api-migrations:v1.22.0/add-embedding-column.sql runOnChange:true

alter table feed_data
    add column if not exists embedding double precision[] default '{}'::double precision[];
