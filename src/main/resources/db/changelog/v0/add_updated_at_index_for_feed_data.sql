--liquibase formatted sql

--changeset event-api-migrations:add_updated_at_index_for_feed_data runOnChange:false

create index on feed_data (updated_at);
