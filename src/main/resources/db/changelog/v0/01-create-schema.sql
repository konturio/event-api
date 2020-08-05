--liquibase formatted sql

--changeset event-api-migrations:01-create-schema runOnChange:true
CREATE TABLE IF NOT EXISTS event_data_lake
(
    observation_id uuid unique,
    external_id    text,
    created_on     timestamptz,
    updated_on     timestamptz,
    loaded_on      timestamptz,
    provider       varchar(16),
    data           text,

    CONSTRAINT hazard_episode_number UNIQUE (external_id, provider, updated_on)
);

CREATE INDEX ON event_data_lake (observation_id);
CREATE INDEX ON event_data_lake (provider, updated_on);


CREATE TABLE IF NOT EXISTS normalized_records
(
--     HpSrv Hazards
    observation_id     uuid unique,
    provider           text,
    geometry           geometry,
    loaded_on          timestamptz,
    app_id             int,
    autoexpire         bool,
    category_id        text,
    charter_uri        text,
    comment_text       text,
    created_on         timestamptz,
    creator            text,
    ended_on           timestamptz,
    glide_uri          text,
    external_id        text,
    hazard_name        text,
    last_updated_on    timestamptz,
    point              geometry,
    master_incident_id text,
    message_id         text,
    org_id             int,
    severity_id        text,
    snc_url            text,
    started_on         timestamptz,
    status             text,
    type_id            text,
    updated_on         timestamptz,
    update_user        text,
    product_total      text,
    uuid               uuid,
    in_dashboard       text,
    areabrief_url      text,
    description        text,

--     HpSrv Mags
    mag_id             int,
    mag_uuid           uuid,
    mag_updated_on     timestamptz,
    mag_created_on     timestamptz,
    title              text,
    mag_type           text,
    is_active          bool
);

CREATE INDEX ON normalized_records (external_id, updated_on);

CREATE TABLE IF NOT EXISTS combined_events
(
    id             bigint generated always as identity primary key,
    observation_id uuid unique,
    type           text not null,
    name           text,
    description    text,
    started_on     timestamptz,
    ended_on       timestamptz
);

CREATE INDEX ON combined_events (observation_id);

CREATE TABLE IF NOT EXISTS combined_episodes
(
    id             bigint generated always as identity primary key,
    event_id       bigint not null references combined_events (id) on delete cascade,
    observation_id uuid unique,
    description    text,
    occurred_on    timestamptz,
    loaded_on      timestamptz,
    provider       text
);

CREATE INDEX ON combined_episodes (observation_id);

CREATE TABLE IF NOT EXISTS combined_areas
(
    id         bigint generated always as identity primary key,
    episode_id bigint not null references combined_episodes (id) on delete cascade,
    severity   text   not null,
    geometry   geometry
);
