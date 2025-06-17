--liquibase formatted sql

--changeset event-api-migrations:v1.22.0/add-forecasted-column.sql runOnChange:false
alter table feed_data add column if not exists forecasted boolean default false;
