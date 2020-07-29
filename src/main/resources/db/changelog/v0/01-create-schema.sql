--liquibase formatted sql

--changeset event-api-migrations:01-create-schema
CREATE TABLE IF NOT EXISTS event_data_lake
(
    observation_id uuid,
    hazard_id      text,
    create_date    timestamptz,
    update_date    timestamptz,
    upload_date    timestamptz,
    provider       varchar(16),
    data           text,

    CONSTRAINT hazard_episode_number UNIQUE (hazard_id, provider, update_date)
);

CREATE INDEX ON event_data_lake (observation_id);
CREATE INDEX ON event_data_lake (provider, update_date);


CREATE TABLE IF NOT EXISTS normalized_records
(
--     HpSrv Hazards
    observation_id     uuid,
    provider           text,
    geometry           geometry,
    app_id             int,
    autoexpire         bool,
    category_id        text,
    charter_uri        text,
    comment_text       text,
    create_date        timestamptz,
    creator            text,
    end_date           timestamptz,
    glide_uri          text,
    hazard_id          text,
    hazard_name        text,
    last_update        timestamptz,
    point              geometry,
    master_incident_id text,
    message_id         text,
    org_id             int,
    severity_id        text,
    snc_url            text,
    start_date         timestamptz,
    status             text,
    type_id            text,
    update_date        timestamptz,
    update_user        text,
    product_total      text,
    uuid               uuid,
    in_dashboard       text,
    areabrief_url      text,
    description        text,

--     HpSrv Mags
    mag_id             int,
    mag_uuid           uuid,
    mag_update_date    timestamptz,
    mag_create_date    timestamptz,
    title              text,
    mag_type           text,
    is_active          bool
);