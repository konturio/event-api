--liquibase formatted sql

--changeset event-api-migrations:v1.10/update-unknown-severity-values.sql runOnChange:false

with events as (
    select
        feed_id,
        event_id,
        version as "version",
        (
            select episode ->> 'severity'
            from jsonb_array_elements(episodes) episode
            where episode ->> 'severity' in ('EXTREME', 'SEVERE', 'MODERATE', 'MINOR', 'TERMINATION')
            order by (episode ->> 'updatedAt')::timestamptz desc
            limit 1
        ) as latest_severity
    from feed_data
    where latest_severity = 'UNKNOWN' and severities && array['EXTREME', 'SEVERE', 'MODERATE', 'MINOR', 'TERMINATION']
)
update feed_data fd
set
    latest_severity = e.latest_severity
from events e
where fd.feed_id = e.feed_id
  and fd.event_id = e.event_id
  and fd.version = e.version;
