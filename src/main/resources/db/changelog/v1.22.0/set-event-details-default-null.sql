--liquibase formatted sql
--changeset event-api-migrations:v1.22.0/set-event-details-default-null.sql runOnChange:false

alter table feed_data
    alter column event_details set default null;
update feed_data
set event_details = null
where event_details = 'null'::jsonb;
