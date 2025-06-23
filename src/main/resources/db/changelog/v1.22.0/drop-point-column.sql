--liquibase formatted sql

--changeset event-api-migrations:v1.22.0/drop-point-column.sql runOnChange:false

alter table normalized_observations
    drop column if exists point;
