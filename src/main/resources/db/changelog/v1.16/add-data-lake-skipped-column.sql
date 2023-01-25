--liquibase formatted sql

--changeset event-api-migrations:v1.16/add-data-lake-skipped-column.sql runOnChange:true

alter table data_lake add column skipped boolean default false;