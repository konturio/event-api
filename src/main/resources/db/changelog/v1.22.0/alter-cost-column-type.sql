--liquibase formatted sql

--changeset event-api-migrations:v1.22.0/alter-cost-column-type.sql runOnChange:false

alter table normalized_observations
    alter column cost type jsonb using case when cost is null then null else to_jsonb(cost) end;
