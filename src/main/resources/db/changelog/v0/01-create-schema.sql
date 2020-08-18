--liquibase formatted sql

--changeset event-api-migrations:01-create-schema runOnChange:true
CREATE TABLE IF NOT EXISTS data_lake
(
    observation_id uuid unique,
    external_id    text,
    updated_at     timestamptz,
    loaded_at      timestamptz,
    provider       text,
    data           text,

    UNIQUE (external_id, provider, updated_at)
);

CREATE TABLE IF NOT EXISTS normalized_observations
(
    observation_id      uuid unique references data_lake (observation_id),
    external_id         text,
    provider            text,
    point               geometry, -- centroid
    geometries          jsonb,    -- featurecollection with area geometry:
    event_severity      text,
    name                text,
    description         text,
    episode_description text,
    type                text,
    active              boolean,
    cost                numeric,  -- EM-DAT
    region              text,     -- EM-DAT
    loaded_at           timestamptz,
    started_at          timestamptz,
    ended_at            timestamptz,
    updated_at          timestamptz,
    source_uri          text
);

CREATE INDEX ON normalized_observations (external_id);

CREATE TABLE IF NOT EXISTS kontur_events
(
    event_id       uuid,
    version        bigint,
    observation_id uuid references normalized_observations (observation_id),

    UNIQUE (event_id, version, observation_id)
);

CREATE TABLE IF NOT EXISTS feeds
(
    feed_id     uuid primary key,
    description text,
    alias       text unique,
    providers   text[],
    roles       text[]
);

CREATE TABLE IF NOT EXISTS feed_data
(
    event_id     uuid,
    feed_id      uuid,
    version      bigint,
    name         text,
    description  text,
    started_at   timestamptz,
    ended_at     timestamptz,
    updated_at   timestamptz,
    observations jsonb,
    episodes     jsonb,

    UNIQUE (event_id, feed_id, version)
);

CREATE INDEX ON feed_data (event_id, version);
create index on feed_data (feed_id);
