--liquibase formatted sql

--changeset event-api-migrations:v1.15/recalculate-type-column.sql runOnChange:true

with events as (
    select
        feed_id,
        event_id,
        version as "version",
        (
            select episode ->> 'type'
            from jsonb_array_elements(episodes) episode
            order by (episode ->> 'updatedAt')::timestamptz desc
            limit 1
        ) as latest_type
    from feed_data
    where type is null
)
update feed_data fd
set
    type = e.latest_type
from events e
where fd.feed_id = e.feed_id
  and fd.event_id = e.event_id
  and fd.version = e.version;