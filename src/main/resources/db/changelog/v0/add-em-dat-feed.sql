--liquibase formatted sql

--changeset event-api-migrations:v0/add-em-dat-feed.sql runOnChange:false

insert into feeds (feed_id, alias, description, providers)
values (uuid_generate_v4(), 'em-dat', 'EM-DAT', '{"em-dat"}');

