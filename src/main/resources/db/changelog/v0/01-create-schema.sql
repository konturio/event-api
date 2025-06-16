--liquibase formatted sql

--changeset event-api-migrations:01-create-schema runOnChange:false
create table if not exists data_lake
(
    observation_id uuid unique,
    external_id    text,
    updated_at     timestamptz,
    loaded_at      timestamptz,
    provider       text,
    data           text,

    unique (external_id, provider, updated_at)
);

create table if not exists normalized_observations
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

create index on normalized_observations (external_id);

create table if not exists kontur_events
(
    event_id       uuid,
    version        bigint,
    observation_id uuid,

    unique (event_id, version, observation_id)
);

create table if not exists feeds
(
    feed_id     uuid primary key,
    description text,
    alias       text unique,
    providers   text[],
    roles       text[]
);

create table if not exists feed_data
(
    event_id     uuid,
    feed_id      uuid,
    version      bigint,
    name         text,
    description  text,
    started_at   timestamptz,
    ended_at     timestamptz,
    updated_at   timestamptz,
    observations uuid[],
    episodes     jsonb,

    unique (event_id, feed_id, version)
);

create index on feed_data (event_id, version);
create index on feed_data (feed_id);
