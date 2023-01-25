--liquibase formatted sql

--changeset event-api-migrations:v1.16/add-normalized-observations-loss-column.sql runOnChange:true

alter table normalized_observations
add column loss jsonb default '{}';