--liquibase formatted sql

--changeset event-api-migrations:v1.22.0/add-event-severity-data-column.sql runOnChange:true

alter table feed_data
    add column event_severity_data jsonb default '{}'::jsonb;

update feed_data
set event_severity_data = severity_data
where event_severity_data = '{}'::jsonb or event_severity_data is null;
