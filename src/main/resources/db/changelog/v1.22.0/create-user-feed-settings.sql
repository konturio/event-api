--liquibase formatted sql

--changeset event-api-migrations:v1.22.0/create-user-feed-settings.sql runOnChange:true

create table if not exists user_feed_settings (
    user_name text primary key,
    feeds text[] default '{}'::text[],
    default_feed text
);

create index if not exists user_feed_settings_default_feed_idx on user_feed_settings (default_feed);
