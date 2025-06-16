--liquibase formatted sql

--changeset event-api-migrations:v1.15/create-new-feed-data-table.sql runOnChange:true

drop table if exists public.severities cascade;

create table public.severities
(
    severity_id smallserial not null,
    severity    text,
    unique (severity_id)
);

insert into public.severities(severity_id, severity)
values (-1, 'UNKNOWN'),
       (0, 'TERMINATION'),
       (1, 'MINOR'),
       (2, 'MODERATE'),
       (3, 'SEVERE'),
       (4, 'EXTREME');

drop table if exists public.feed_data_upd;

create table public.feed_data_upd
(
    event_id            uuid,                                               --8+8
    feed_id             uuid,                                               --8+8
    version             smallint,                                           --2
    enrichment_attempts smallint                 default 0,                 --2
    severity_id         smallint                 default -1,                --2
    is_latest_version   boolean                  default true,              --1
    enriched            boolean                  default false,             --1
    updated_at          timestamp with time zone,                           --8
    started_at          timestamp with time zone,                           --8
    ended_at            timestamp with time zone,                           --8
    composed_at         timestamp with time zone default current_timestamp, --8
    enriched_at         timestamp with time zone,                           --8
    enrichment_skipped  boolean                  default false,             --1
    type                text,
    name                text,
    description         text,
    episodes            jsonb,
    observations        uuid[],
    collected_geometry  public.geometry generated always as (public.collectgeometryfromepisodes(episodes)) stored,
    event_details       jsonb                    default '{}'::jsonb,
    geometries          jsonb,
    urls                text[]                   default '{}'::text[],
    proper_name         text,
    location            text,
    foreign key (feed_id)
        references public.feeds (feed_id),
    foreign key (severity_id)
        references public.severities (severity_id),
    unique (event_id, version, feed_id)
);

insert into public.feed_data_upd
(event_id,
 feed_id,
 version,
 enrichment_attempts,
 severity_id,
 is_latest_version,
 enriched,
 updated_at,
 started_at,
 ended_at,
 composed_at,
 enriched_at,
 enrichment_skipped,
 type,
 name,
 description,
 episodes,
 observations,
 event_details,
 geometries,
 urls,
 proper_name,
 location)
select event_id,
       feed_id,
       version,
       enrichment_attempts,
       case
           when (severity = 'UNKNOWN' or severity is null) then -1::smallint
           when severity = 'TERMINATION' then 0::smallint
           when severity = 'MINOR' then 1::smallint
           when severity = 'MODERATE' then 2::smallint
           when severity = 'SEVERE' then 3::smallint
           when severity = 'EXTREME' then 4::smallint
end severity_id,
       is_latest_version,
       enriched,
       updated_at,
       started_at,
       ended_at,
       composed_at,
       enriched_at,
       enrichment_skipped,
       type,
       name,
       description,
       episodes,
       observations,
       event_details,
       geometries,
       urls,
       proper_name,
       location
from feed_data;

create index feed_data_collected_geometry_gist_idx
    on feed_data_upd
    using gist (collected_geometry)
    where (is_latest_version and enriched);

create index feed_data_composed_at_btree_idx
    on feed_data_upd
    using btree (composed_at);

create index feed_data_enriched_at_btree_idx
    on feed_data_upd
    using btree (enriched_at);

create index feed_data_enrichment_skipped_btree_idx
    on feed_data_upd
    using btree (enrichment_skipped);

create index feed_data_collected_geometry_severity_id_gist_idx
    on feed_data_upd
    using gist (collected_geometry, severity_id)
    where is_latest_version and enriched;

create index feed_data_severity_id_updated_at_btree_gist_idx
    on feed_data_upd
    using btree (severity_id, updated_at)
    where is_latest_version and enriched;

create index feed_data_updated_at_btree_idx
    on feed_data_upd
    using btree (updated_at);

create index feed_data_updated_at_is_not_enriched_btree_idx
    on feed_data_upd
    using btree (enrichment_attempts nulls first, updated_at)
    where not enriched;

create index feed_data_updated_at_latest_version_enriched_btree_idx
    on feed_data_upd
    using btree (updated_at)
    where not is_latest_version and enriched;
