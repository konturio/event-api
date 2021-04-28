--liquibase formatted sql

--changeset event-api-migrations:v0/add-feed-data-is-latest-version-column.sql runOnChange:false
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT count(*) FROM information_schema.columns WHERE table_name = 'feed_data' AND column_name = 'is_latest_version';

alter table feed_data
    add is_latest_version bool default false;

with events as (
    select feed_id, event_id, max(version) as "version"
    from feed_data
    group by feed_id, event_id
)
update feed_data fd
set is_latest_version = true
from events e
where fd.feed_id = e.feed_id
  and fd.event_id = e.event_id
  and fd.version = e.version;

drop index if exists feed_data_updated_at_idx;

CREATE INDEX if not exists feed_data_updated_at_feed_id_is_latest_version_idx ON public.feed_data USING btree (updated_at, feed_id) where is_latest_version;