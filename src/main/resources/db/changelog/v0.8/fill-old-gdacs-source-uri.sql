--liquibase formatted sql

--changeset event-api-migrations:v0.8/fill-old-gdacs-source-uri.sql runOnChange:true

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
episodes_to_update as (
    select fd.feed_id, fd.event_id, fd.version,
           to_jsonb((
               select array_agg(jsonb_set(ep, '{urls}', (
                   select coalesce((
                       select jsonb_agg(distinct source_uri)
                       from normalized_observations
                       where source_uri is not null and observation_id in (select unnest((
                           select array_agg(obs_id::uuid) from jsonb_array_elements_text(ep -> 'observations') obs_id)))
                       ), '[]'::jsonb)),
                   true))
               from jsonb_array_elements(episodes) ep
           )) episodes_with_links
    from feed_data fd
    inner join event_ids_to_update eiu on fd.event_id = eiu.event_id
    where feed_id = (select feed_id from feeds where alias = 'disaster-ninja-02')
),
events_to_update as (
    select eu.*, (
        select (select array_agg(last_ep_links) from jsonb_array_elements_text(ep -> 'urls') last_ep_links)
        from jsonb_array_elements(episodes_with_links) ep
        where ep -> 'urls' <> '[]'::jsonb
        order by (ep ->> 'startedAt')::timestamptz desc, (ep ->> 'updatedAt')::timestamptz desc
        limit 1
    ) links
    from episodes_to_update eu
)
update feed_data fd set urls = eu.links, episodes = eu.episodes_with_links
from (select * from events_to_update) eu
where fd.event_id = eu.event_id and fd.feed_id = eu.feed_id and fd.version = eu.version;