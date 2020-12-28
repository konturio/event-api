--liquibase formatted sql

--changeset event-api-migrations:add_feed_event_status runOnChange:false

CREATE TABLE feed_event_status
(
    feed_id     uuid references feeds (feed_id),
    event_id    uuid,
    actual      boolean,

    UNIQUE (feed_id, event_id)
);

CREATE INDEX ON feed_event_status (feed_id, actual);

DELETE FROM feed_data;

INSERT INTO feed_event_status (event_id, feed_id, actual)
SELECT ke.event_id, f.feed_id, false
FROM kontur_events ke, feeds f
WHERE ke.provider = ANY(f.providers)
GROUP BY ke.event_id, f.feed_id




