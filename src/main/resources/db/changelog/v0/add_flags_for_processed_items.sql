--liquibase formatted sql

--changeset event-api-migrations:v0/add_flags_for_processed_items.sql runOnChange:false
alter table data_lake add column normalized boolean;
update data_lake dl set normalized = exists(select 1 from normalized_observations nr where dl.observation_id = nr.observation_id);

alter table normalized_observations add column recombined boolean;
update normalized_observations no set recombined = exists(select 1 from kontur_events ke where ke.observation_id = no.observation_id);
