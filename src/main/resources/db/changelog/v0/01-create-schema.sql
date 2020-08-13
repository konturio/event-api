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
    observation_id uuid unique references data_lake (observation_id),
    external_id    text,
    provider       text,
    point          geometry, -- centroid
    geometries     jsonb,    -- featurecollection with area geometry:
    event_severity text,
    name           text,
    description    text,
    type           text,
    cost           numeric,  -- EM-DAT
    region         text,     -- EM-DAT
    loaded_at      timestamptz,
    source_uri     text
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
    providers   text[],
    roles       text[]
);

CREATE TABLE IF NOT EXISTS feed_data
(
    event_id     uuid,
    version      bigint,
    name         text,
    description  text,
    observations jsonb, -- array of observations as json
    -- select to_json(r.*) from (select * from observations where ... order by ts);
    episodes     jsonb -- array of episodes as json
)

--
-- CREATE TABLE IF NOT EXISTS combined_events
-- (
--     id             bigint generated always as identity primary key,
--     observation_id uuid unique,
--     type           text not null,
--     name           text,
--     description    text,
--     started_on     timestamptz,
--     ended_on       timestamptz
-- );
--
-- CREATE INDEX ON combined_events (observation_id);
--
-- CREATE TABLE IF NOT EXISTS combined_episodes
-- (
--     id             bigint generated always as identity primary key,
--     event_id       bigint not null references combined_events (id) on delete cascade,
--     observation_id uuid unique,
--     description    text,
--     occurred_on    timestamptz,
--     loaded_on      timestamptz,
--     provider       text
-- );
--
-- CREATE INDEX ON combined_episodes (observation_id);
--
-- CREATE TABLE IF NOT EXISTS combined_areas
-- (
--     id         bigint generated always as identity primary key,
--     episode_id bigint not null references combined_episodes (id) on delete cascade,
--     severity   text   not null,
--     geometry   geometry
-- );
