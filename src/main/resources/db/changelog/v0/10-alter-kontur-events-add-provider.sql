--liquibase formatted sql

--changeset event-api-migrations:09-clear-hpsrv-hazards-and-feed-data runOnChange:false

alter table kontur_events add COLUMN provider text;

update kontur_events ke set provider = (select provider from data_lake dl where dl.observation_id = ke.observation_id);