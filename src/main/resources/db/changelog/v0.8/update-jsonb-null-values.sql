--liquibase formatted sql

--changeset event-api-migrations:v0.8/update-jsonb-null-values.sql runOnChange:false

update feed_data
set event_details = null
where event_details = 'null'::jsonb;

