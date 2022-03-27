--liquibase formatted sql

--changeset event-api-migrations:v1.5/alter-observations-change-urls-type-to-array.sql runOnChange:false

alter table normalized_observations alter column source_uri type text[] using array[source_uri];
alter table normalized_observations alter column source_uri set default '{}'::text[];
