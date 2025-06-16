--liquibase formatted sql

--changeset event-api-migrations:v1.22.0/create-merge-operations-table.sql runOnChange:true
create table if not exists merge_operations (
    merge_operation_id bigserial primary key,
    event_ids text[] not null,
    confidence double precision,
    approved boolean default false,
    decision_made_by text,
    executed boolean default false,
    decision_made_at timestamp with time zone,
    taken_to_merge_at timestamp with time zone
);
create unique index if not exists merge_operations_event_ids_unique
    on merge_operations ((array(select unnest(event_ids) order by 1)));
create index if not exists merge_operations_taken_merge_idx
    on merge_operations (taken_to_merge_at asc nulls first);
create index if not exists merge_operations_approved_pending_idx
    on merge_operations (decision_made_at asc nulls first)
    where approved and not executed;
