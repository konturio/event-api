--liquibase formatted sql

--changeset event-api-migrations:v1.22.0/add-trgm-indexes.sql runOnChange:false

create extension if not exists pg_trgm;

create index if not exists feed_data_name_trgm_idx
    on feed_data using gist (name gist_trgm_ops);

create index if not exists feed_data_proper_name_trgm_idx
    on feed_data using gist (proper_name gist_trgm_ops);

create index if not exists feed_data_description_trgm_idx
    on feed_data using gist (description gist_trgm_ops);
