--liquibase formatted sql

--changeset event-api-migrations:v1.7/add-index-to-feed-data-by-updated-at.sql runOnChange:false

create index feed_data_updated_at_idx on feed_data (updated_at);
