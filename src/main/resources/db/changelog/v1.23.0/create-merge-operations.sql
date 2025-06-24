--liquibase formatted sql

--changeset event-api-migrations:v1.23.0/create-merge-operations.sql runOnChange:false
create table if not exists merge_operations (
    merge_operation_id serial primary key,
    event_ids text[] not null,
    confidence double precision not null,
    approved boolean default false,
    decision_made_by text,
    executed boolean default false,
    decision_made_at timestamp with time zone,
    taken_to_merge_at timestamp with time zone
);
create unique index if not exists merge_operations_event_ids_idx on merge_operations (event_ids);
create index if not exists merge_operations_taken_idx on merge_operations (taken_to_merge_at asc nulls first);
create index if not exists merge_operations_approved_idx on merge_operations (approved, executed, decision_made_at asc);
