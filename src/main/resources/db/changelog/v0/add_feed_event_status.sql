--liquibase formatted sql

--changeset event-api-migrations:add_feed_event_status runOnChange:false

create table feed_event_status
(
    feed_id     uuid references feeds (feed_id),
    event_id    uuid,
    actual      boolean,

    unique (feed_id, event_id)
);

create index on feed_event_status (feed_id, actual);

delete from feed_data;

insert into feed_event_status (event_id, feed_id, actual)
select ke.event_id, f.feed_id, false
from kontur_events ke, feeds f
where ke.provider = ANY(f.providers)
group by ke.event_id, f.feed_id




