--liquibase formatted sql

--changeset event-api-migrations:17-insert-gdacs-feed runOnChange:false

insert into feeds (feed_id, alias, description, providers)
values (uuid_generate_v4(), 'gdacs', 'Global Disaster Alert and Coordination System feed', '{"gdacsAlert", "gdacsAlertGeometry"}');
