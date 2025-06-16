--liquibase formatted sql

--changeset event-api-migrations:21-insert-firms-feed.sql runOnChange:false

insert into feeds (feed_id, alias, description, providers)
values (uuid_generate_v4(), 'firms', 'Fire Information for Resource Management System', '{"firms.modis-c6", "firms.suomi-npp-viirs-c2", "firms.noaa-20-viirs-c2"}');
