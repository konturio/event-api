--liquibase formatted sql

--changeset event-api-migrations:v0.8/fill-old-gdacs-source-uri.sql runOnChange:false

update normalized_observations no
set source_uri = dl.link
from (
    select observation_id,
           replace((xpath(
               '//cap:parameter[cap:valueName="link"]/cap:value/text()',
               xmlparse(document data),
               array[array['cap', 'urn:oasis:names:tc:emergency:cap:1.2']]
           ))[1]::text, 'amp;', '') link
    from data_lake
    where provider = 'gdacsAlert'
) dl
where no.observation_id = dl.observation_id;


update normalized_observations no
set source_uri = dl.link
from (
     select observation_id,
            (data::jsonb) -> 'features' -> 0 -> 'properties' -> 'url' ->> 'report' link
     from data_lake
     where provider = 'gdacsAlertGeometry'
) dl
where no.observation_id = dl.observation_id;


with event_ids_to_update as (
    select distinct event_id
    from kontur_events
    where provider in ('gdacsAlert', 'gdacsAlertGeometry')
),
events_to_update as (
    select fd.feed_id, fd.event_id, fd.version,
        (
            select coalesce(array_agg(distinct source_uri), '{}'::text[])
            from normalized_observations
            where source_uri is not null and observation_id in (select unnest(fd.observations))
        ) links,
        to_jsonb((
            select array_agg(jsonb_set(ep, '{urls}', (
                select coalesce((
                    select jsonb_agg(distinct source_uri)
                    from normalized_observations
                    where source_uri is not null and observation_id in (select unnest((
                        select array_agg(obs_id::uuid) from jsonb_array_elements_text(ep -> 'observations') obs_id)))
                ), '[]'::jsonb)),
                true
            )) from jsonb_array_elements(episodes) ep
        )) episodes_with_urls
    from feed_data fd
    inner join event_ids_to_update eiu on fd.event_id = eiu.event_id
    where fd.urls = '{}'::text[]
)
update feed_data fd set urls = eu.links, episodes = eu.episodes_with_urls
from (select * from events_to_update) eu
where fd.event_id = eu.event_id and fd.feed_id = eu.feed_id and fd.version = eu.version;
