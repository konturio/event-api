--liquibase formatted sql

--changeset event-api-migrations:v0.8/fill-old-gdacs-proper-name.sql runOnChange:false

update normalized_observations no
set proper_name = dl.event_name
from (
    select observation_id,
           btrim((xpath('//cap:parameter[cap:valueName="eventname"]/cap:value/text()', xmlparse(document data),
               array[array['cap', 'urn:oasis:names:tc:emergency:cap:1.2']]))[1]::text) event_name
    from data_lake
    where provider = 'gdacsAlert'
) dl
where no.observation_id = dl.observation_id;


update normalized_observations no
set proper_name = dl.event_name
from (
    select observation_id,
           btrim((data::jsonb) -> 'features' -> 0 -> 'properties' ->> 'eventname') event_name
    from data_lake
    where provider = 'gdacsAlertGeometry'
) dl
where no.observation_id = dl.observation_id;


with event_ids_to_update as (
    select distinct event_id
    from kontur_events
    where provider in ('gdacsAlert', 'gdacsAlertGeometry')
),
episodes_to_update as (
    select fd.feed_id, fd.event_id, fd.version,
           to_jsonb((
               select array_agg(jsonb_set(ep, '{properName}', (
                   select to_jsonb(no.proper_name)
                   from normalized_observations no
                   where no.proper_name is not null and observation_id in (select unnest((
                       select array_agg(obs_id::uuid) from jsonb_array_elements_text(ep -> 'observations') obs_id)))
                   order by no.provider
                   limit 1),
                   true
               )) from jsonb_array_elements(episodes) ep
           )) episodes_with_proper_names
    from feed_data fd inner join event_ids_to_update eiu on fd.event_id = eiu.event_id
    where feed_id = (select feed_id from feeds where alias = 'disaster-ninja-02')
),
events_to_update as (
    select eu.*, (
        select ep ->> 'properName'
        from jsonb_array_elements(episodes_with_proper_names) ep
        where ep ->> 'properName' is not null and ep ->> 'properName' <> ''
        order by (ep ->> 'startedAt')::timestamptz, (ep ->> 'updatedAt')::timestamptz
        limit 1
    ) as event_proper_name
    from episodes_to_update eu
)
update feed_data fd set proper_name = eu.event_proper_name, episodes = eu.episodes_with_proper_names
    from (select * from events_to_update) eu
where fd.event_id = eu.event_id and fd.feed_id = eu.feed_id and fd.version = eu.version;


