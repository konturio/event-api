--liquibase formatted sql

--changeset event-api-migrations:v1.15/recalculate-events.sql runOnChange:true

update data_lake set normalized = false where provider in ('gdacsAlert', 'gdacsAlertGeometry', 'kontur.events');