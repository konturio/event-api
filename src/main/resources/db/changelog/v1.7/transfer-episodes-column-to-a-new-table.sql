--liquibase formatted sql

--changeset event-api-migrations:v1.7/transfer-episodes-column-to-a-new-table.sql runOnChange:false

CREATE TYPE episodes_type as (
    name                text,
    description         text,
    type                text,
    severity            text,
    "startedAt"         timestamptz,
    "endedAt"           timestamptz,
    "updatedAt"         timestamptz,
    "sourceUpdatedAt"   timestamptz,
    urls                text[],
    location            text,
    active              boolean,
    observations        uuid[],
    geometries          jsonb
);

create or replace procedure migrate_episodes(days integer)
    language plpgsql
as
'
declare
    startTime timestamptz;
    endTime timestamptz;
    untilTime timestamptz;
    i integer;
begin
    i = 0;
    untilTime = now() + interval ''1 day'';
    startTime = (select max(updated_at) from feed_episode);
    if (startTime is null) then
        startTime = (select min(updated_at) from feed_data where episodes is not null) - interval ''1 second'';
    end if;

    while (startTime < untilTime and (days <= 0 or i < days))
    loop
        endTime = startTime + interval ''1 day'';

        insert into feed_episode
        select
            e.event_id, e.feed_id, e.version,
            row_number() over (PARTITION BY  e.event_id, e.feed_id, e.version ) as episode_number,
                e.name, e.description, e.type, e.severity,
            e."startedAt" as started_at, e."endedAt" as ended_at, e."updatedAt" as updated_at, e."sourceUpdatedAt" as source_updated_at,
            e.proper_name, e.urls, e.location, e.active, e.episode_details, e.enriched, e.observations, e.geometries
        from (select fd.version as version,
                     fd.event_id as event_id,
                     fd.feed_id as feed_id,
                     fd.proper_name as proper_name,
                     fd.event_details as episode_details,
                     fd.enriched as enriched,
                     (jsonb_populate_recordset(null::episodes_type, fd.episodes)).*
              from feed_data fd
              where fd.episodes is not null
                and fd.updated_at > startTime and fd.updated_at <= endTime
             ) e
        order by e.feed_id, e.event_id, e.version, e."startedAt", e."sourceUpdatedAt";

        --commit;
        startTime = endTime;
        i = i + 1;
    end loop;
end;
';


call migrate_episodes(0);

drop type if exists episodes_type;