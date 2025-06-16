--liquibase formatted sql

--changeset event-api-migrations:v1.22.0/create-merged-groups-table.sql runOnChange:false

create table if not exists merged_groups (
    merge_group_id uuid not null,
    event_id uuid not null,
    primary_idx numeric,
    unique(event_id)
);

create index if not exists merged_groups_merge_group_id_idx on merged_groups (merge_group_id);
