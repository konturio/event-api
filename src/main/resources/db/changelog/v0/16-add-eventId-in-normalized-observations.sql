--liquibase formatted sql

--changeset event-api-migrations:16-add-eventId-in-normalized-observations:false

alter table normalized_observations add column external_episode_id text;