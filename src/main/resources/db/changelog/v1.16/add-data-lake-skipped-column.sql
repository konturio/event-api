--liquibase formatted sql

--changeset event-api-migrations:v1.16/add-data-lake-skipped-column.sql runOnChange:true

alter table data_lake add column skipped boolean default false;

create index if not exists data_lake_normalized_skipped_idx
on data_lake (provider, loaded_at)
where (normalized is false and skipped is false);