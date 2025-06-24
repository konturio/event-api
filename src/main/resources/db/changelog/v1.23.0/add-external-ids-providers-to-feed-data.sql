--liquibase formatted sql

--changeset event-api-migrations:v1.23.0/add-external-ids-providers-to-feed-data.sql runOnChange:false
alter table feed_data
    add column if not exists external_event_ids text[] default '{}',
    add column if not exists providers text[] default '{}';
update feed_data fd
set external_event_ids = (select array_agg(distinct no.external_event_id)
                          from normalized_observations no
                          where no.observation_id = any(fd.observations))
where fd.feed_id in (select feed_id from feeds where alias in ('wildfire.calfire','wildfire.inciweb','wildfire.perimeters.nifc','wildfire.locations.nifc'))
  and (fd.external_event_ids = '{}' or fd.external_event_ids is null);
update feed_data fd
set providers = (select array_agg(distinct no.provider)
                 from normalized_observations no
                 where no.observation_id = any(fd.observations))
where fd.feed_id in (select feed_id from feeds where alias in ('wildfire.calfire','wildfire.inciweb','wildfire.perimeters.nifc','wildfire.locations.nifc'))
  and (fd.providers = '{}' or fd.providers is null);
