--liquibase formatted sql

--changeset event-api-migrations:add_updated_at_index_for_feed_data runOnChange:false

CREATE INDEX ON feed_data (updated_at);
