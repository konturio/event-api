--liquibase formatted sql

--changeset event-api-migrations:06-add-constraints runOnChange:false

alter table feed_data
    add constraint fk_feeds
        foreign key (feed_id)
            references feeds (feed_id);