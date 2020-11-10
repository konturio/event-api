--liquibase formatted sql

--changeset event-api-migrations:14-clear-feed-data-kontur-events-normalized-observations runOnChange:false

delete from feed_data;
delete from kontur_events;
delete from normalized_observations;

