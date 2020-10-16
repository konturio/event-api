--liquibase formatted sql

--changeset event-api-migrations:11-clear-sqs-and-feed-data runOnChange:false

delete from normalized_observations where provider = 'pdcSqs';
delete from feed_data;