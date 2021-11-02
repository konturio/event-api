--liquibase formatted sql

--changeset event-api-migrations:v0.8/fill-old-gdacs-location.sql runOnChange:true

update normalized_observations no
set region = dl.country
from (
    select observation_id,
    btrim((xpath('//cap:parameter[cap:valueName="country"]/cap:value/text()', xmlparse(document data),
    array[array['cap', 'urn:oasis:names:tc:emergency:cap:1.2']]))[1]::text) country
    from data_lake
    where provider = 'gdacsAlert'
    ) dl
where no.observation_id = dl.observation_id;


update normalized_observations no
set region = dl.country
from (
    select observation_id,
    btrim((data::jsonb) -> 'features' -> 0 -> 'properties' ->> 'country') country
    from data_lake
    where provider = 'gdacsAlertGeometry'
    ) dl
where no.observation_id = dl.observation_id;


with event_ids_to_update as (
    select distinct event_id
    from kontur_events
    where provider in ('gdaacsAlert', 'gdacsAlertGeometry')
),
episodes_to_update as (
    select fd.feed_id, fd.event_id, fd.version,
           to_jsonb((
               select array_agg(jsonb_set(ep, '{location}', (
                   select to_jsonb(no.region)
                   from normalized_observations no
                   where no.region is not null and observation_id in (select unnest((
                       select array_agg(obs_id::uuid) from jsonb_array_elements_text(ep -> 'observations') obs_id)))
                   order by no.provider
                   limit 1),
                   true
               )) from jsonb_array_elements(episodes) ep
           )) episodes_with_locations
from feed_data fd inner join event_ids_to_update eiu on fd.event_id = eiu.event_id
where feed_id = (select feed_id from feeds where alias = 'disaster-ninja-02')
),
events_to_update as (
    select eu.*, (
        select ep ->> 'location'
        from jsonb_array_elements(episodes_with_locations) ep
        where ep ->> 'location' is not null and ep ->> 'location' <> ''
        order by (ep ->> 'startedAt')::timestamptz desc, (ep ->> 'updatedAt')::timestamptz desc
        limit 1
    ) as event_location
    from episodes_to_update eu
)
update feed_data fd set location = eu.event_location, episodes = eu.episodes_with_locations
    from (select * from events_to_update) eu
where fd.event_id = eu.event_id and fd.feed_id = eu.feed_id and fd.version = eu.version;


