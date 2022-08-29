--liquibase formatted sql

--changeset event-api-migrations:v1.12/add-max-severity-column-to-feed-data.sql runOnChange:false

alter table feed_data
    add if not exists severity text;

with events as (
    select
        feed_id,
        event_id,
        version as "version",
        (
            select
                max(
                    case
                        when s = 'UNKNOWN' then 1
                        when s = 'TERMINATION' then 2
                        when s = 'MINOR' then 3
                        when s = 'MODERATE' then 4
                        when s = 'SEVERE' then 5
                        when s = 'EXTREME' then 6
                        else 0
                    end
                )
            from unnest(severities) s
        ) as severity
    from feed_data
    where is_latest_version
)
update feed_data fd
set
    severity = (
        case
            when e.severity = 1 then 'UNKNOWN'
            when e.severity = 2 then 'TERMINATION'
            when e.severity = 3 then 'MINOR'
            when e.severity = 4 then 'MODERATE'
            when e.severity = 5 then 'SEVERE'
            when e.severity = 6 then 'EXTREME'
        end
    )
from events e
where fd.feed_id = e.feed_id
  and fd.event_id = e.event_id
  and fd.version = e.version;