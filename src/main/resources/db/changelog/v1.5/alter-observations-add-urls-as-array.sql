--liquibase formatted sql

--changeset event-api-migrations:v1.5/alter-observations-add-urls-as-array.sql runOnChange:false

alter table normalized_observations add column urls text[] default '{}'::text[];
