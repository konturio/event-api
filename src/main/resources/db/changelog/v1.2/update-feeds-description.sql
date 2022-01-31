--liquibase formatted sql

--changeset event-api-migrations:v1.2/update-feeds-description.sql runOnChange:false

update feeds
set description = 'The feed contains real-time California wildfires as spots. Historical data starts from 2021.'
where alias = 'test-calfire';

update feeds
set description = 'The feed contains real-time US wildfires as spots and perimeters.'
where alias = 'test-nifc';

update feeds
set description = 'The feed contains real-time data about Cyclones, Droughts, Earthquakes, Floods, Volcanoes, Wildfires.'
where alias = 'disaster-ninja-02';

update feeds
set description = 'The feed contains data available for Swiss Re, which includes real-time and historical hazards.'
where alias = 'swissre-02';

update feeds
set description = 'Public Event Feed contains real-time data about Cyclones, Droughts, Earthquakes, Floods, Volcanoes, Wildfires. It can be used for any purpose, including commercial use.'
where alias = 'kontur-public';

update feeds
set description = 'The feed contains real-time US wildfires as spots.'
where alias = 'test-inciweb';