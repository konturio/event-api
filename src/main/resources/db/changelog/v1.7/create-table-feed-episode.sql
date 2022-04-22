--liquibase formatted sql

--changeset event-api-migrations:v1.7/create-table-feed-episodes.sql runOnChange:false
CREATE TABLE IF NOT EXISTS feed_episode
(
    event_id            uuid,
    feed_id             uuid,
    version             bigint,
    episode_number      bigint,
    name                text,
    description         text,
    type                text,
    severity            text,
    started_at          timestamptz,
    ended_at            timestamptz,
    updated_at          timestamptz,
    source_updated_at   timestamptz,
    proper_name         text,
    urls                text[] default '{}'::text[],
    location            text,
    active              boolean,
    episode_details     jsonb,
    enriched            boolean,
    observations        uuid[],
    geometries          jsonb,

    UNIQUE (event_id, feed_id, version, episode_number)
);

alter table feed_episode add constraint fk_feed_data foreign key (event_id, feed_id, version)
    references feed_data (event_id, feed_id, version);

alter table feed_episode add constraint fk_feeds foreign key (feed_id) references feeds (feed_id);

create index feed_episode_updated_at_idx on feed_episode (updated_at);


