--liquibase formatted sql

--changeset event-api-migrations:v1.10/add-feed-data-severities-columns.sql runOnChange:false

alter table feed_data
    add if not exists latest_severity text;

alter table feed_data
    add if not exists severities text[];

with events as (
    select
        feed_id,
        event_id,
        version as "version",
        (
            select episode ->> 'severity'
            from jsonb_array_elements(episodes) episode
            order by (episode ->> 'updatedAt')::timestamptz desc
            limit 1
        ) as latest_severity,
        (
            select array_agg(distinct episode ->> 'severity')
            from jsonb_array_elements(episodes) episode
        ) as severities
    from feed_data
)
update feed_data fd
set
    latest_severity = e.latest_severity,
    severities = e.severities
from events e
where fd.feed_id = e.feed_id
  and fd.event_id = e.event_id
  and fd.version = e.version;
