--liquibase formatted sql

--changeset event-api-migrations:09-clear-hpsrv-hazards-and-feed-data runOnChange:false

delete from normalized_observations where provider = 'hpSrvSearch';
delete from feed_data;