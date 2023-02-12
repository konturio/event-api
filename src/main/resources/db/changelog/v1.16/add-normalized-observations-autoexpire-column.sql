--liquibase formatted sql

--changeset event-api-migrations:v1.16/add-normalized-observations-autoexpire-column.sql runOnChange:true

alter table normalized_observations
    add column if not exists auto_expire boolean default false;