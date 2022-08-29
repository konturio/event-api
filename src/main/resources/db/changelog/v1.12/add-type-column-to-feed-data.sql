--liquibase formatted sql

--changeset event-api-migrations:v1.12/add-type-column-to-feed-data.sql runOnChange:false

alter table feed_data
    add if not exists type text;

with events as (
    select
        feed_id,
        event_id,
        version as "version",
        (
            case
                when array_length(episode_types, 1) > 1 then (
                    select episode ->> 'type'
                    from jsonb_array_elements(episodes) episode
                    order by (episode ->> 'updatedAt')::timestamptz desc
                    limit 1
                )
                else episode_types[1]
            end
        ) as latest_type
    from feed_data
    where is_latest_version
)
update feed_data fd
set
    type = e.latest_type
from events e
where fd.feed_id = e.feed_id
  and fd.event_id = e.event_id
  and fd.version = e.version;