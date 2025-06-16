--liquibase formatted sql

--changeset event-api-migrations:v1.22.0/alter-cost-column.sql runOnChange:true
alter table normalized_observations
    alter column cost type jsonb using case when cost is null then null else jsonb_build_object('legacy_cost', cost) end;
