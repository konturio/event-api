--changeset event-api-migrations:v1.22.0/sort-feed-data-observations.sql runOnChange:false

-- sort observations array in feed_data table
update feed_data
set observations = (
    select array_agg(o order by o)
    from unnest(observations) o
)
where observations is not null;

-- sort observations arrays inside episodes jsonb
update feed_data
set episodes = (
    select jsonb_agg(
               jsonb_set(ep, '{observations}',
                          coalesce((select jsonb_agg(val order by val)
                                   from jsonb_array_elements_text(ep->'observations') val), '[]'::jsonb),
                          false)
               order by ord)
    from jsonb_array_elements(episodes) with ordinality as e(ep, ord)
)
where episodes is not null;
