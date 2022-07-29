--liquibase formatted sql

--changeset event-api-migrations:v1.10/update-severity-values-for-latest-event-versions-2021-01-01.sql runOnChange:false

with events as (
    select
        feed_id,
        event_id,
        version as "version",
        (
            select episode ->> 'severity'
            from jsonb_array_elements(episodes) episode
            where episode -> 'severity' != 'null'::jsonb
            order by (episode ->> 'updatedAt')::timestamptz desc
            limit 1
        ) as latest_severity,
        (
            select array_agg(distinct episode ->> 'severity')
            from jsonb_array_elements(episodes) episode
            where episode -> 'severity' != 'null'::jsonb
        ) as severities
    from feed_data
    where is_latest_version and updated_at < '2021-01-01'
)
update feed_data fd
set
    latest_severity = e.latest_severity,
    severities = e.severities
from events e
where fd.feed_id = e.feed_id
  and fd.event_id = e.event_id
  and fd.version = e.version;