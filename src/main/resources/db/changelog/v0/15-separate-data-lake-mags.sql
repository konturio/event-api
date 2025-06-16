--liquibase formatted sql

--changeset event-api-migrations:15-separate-data-lake-mags runOnChange:false


create table data_lake_temp as
select uuid_generate_v4() as observation_id,
       external_id,
       updated_at,
       loaded_at + millis_shift::interval as loaded_at,
       provider,
       CONCAT('{"type": "FeatureCollection", "features": [', feature, ']}') as data
from (
         select *,
                concat(
                        (ROW_NUMBER() OVER (PARTITION BY observation_id)::numeric / 1000) ::text,
                        ' milliseconds'
                    ) millis_shift
         from (
                  select *,
                         json_array_elements_text(data::json -> 'features') feature
                  from data_lake
                  where provider = 'hpSrvMag') t
     ) t2;

create unique index on data_lake_temp (loaded_at);

delete from data_lake where provider = 'hpSrvMag';

insert into data_lake select * from data_lake_temp;

drop table data_lake_temp;
