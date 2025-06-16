--liquibase formatted sql

--changeset event-api-migrations:v0/add-feed-data-is-latest-version-column.sql runOnChange:false
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 select count(*) from information_schema.columns where table_name = 'feed_data' AND column_name = 'is_latest_version';

alter table feed_data
    add is_latest_version bool default true;

with events as (
    select feed_id, event_id, max(version) as "version"
    from feed_data
    group by feed_id, event_id
)
update feed_data fd
set is_latest_version = false
from events e
where fd.feed_id = e.feed_id
  and fd.event_id = e.event_id
  and fd.version < e.version;

drop index if exists feed_data_updated_at_idx;

create index if not exists feed_data_updated_at_feed_id_is_latest_version_idx on public.feed_data using btree (updated_at, feed_id) where is_latest_version;
