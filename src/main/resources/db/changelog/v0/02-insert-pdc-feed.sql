--liquibase formatted sql

--changeset event-api-migrations:02-insert-pdc-feed runOnChange:false

insert into feeds (feed_id, alias, description, providers)
values (uuid_generate_v4(), 'pdc', 'Pacific Disaster Center feed', '{"hpSrvMag", "hpSrvSearch"}');
