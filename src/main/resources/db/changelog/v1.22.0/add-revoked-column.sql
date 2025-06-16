--liquibase formatted sql

--changeset event-api-migrations:v1.22.0/add-revoked-column.sql runOnChange:true

alter table feed_data
    add column if not exists revoked boolean default false;
