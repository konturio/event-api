--liquibase formatted sql

--changeset event-api-migrations:v1.1/add-source-updated-at-index-to-normalized-observations.sql runOnChange:false

create extension if not exists btree_gist;

create index if not exists normalized_observations_source_updated_collected_geography_idx
    on normalized_observations using gist (source_updated_at, collected_geography);

