--liquibase formatted sql

--changeset event-api-migrations:v1.15/add-data-lake-skipped-column.sql runOnChange:true

alter table data_lake add column skipped boolean default false;