--liquibase formatted sql

--changeset event-api-migrations:v0/add-feed-data-is-latest-version-column.sql runOnChange:false

alter table feed_data add is_latest_version bool default false;

update feed_data fd set is_latest_version = true where not exists (
        select
        from
            feed_data prev
        where
                fd.feed_id = prev.feed_id
          and fd.event_id = prev.event_id
          and fd.version < prev.version);


drop index if exists feed_data_updated_at_idx;

CREATE INDEX feed_data_updated_at_feed_id_is_latest_version_idx ON public.feed_data USING btree (updated_at, feed_id, is_latest_version);