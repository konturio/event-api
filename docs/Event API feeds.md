# Event API feeds


### List of providers

|     |     |
| --- | --- |
| **Provider name** | **Provider description** |
| hpSrvMag | alert area + attributes (historical hazards only), worldwide |
| hpSrvSearch | central point + attributes (historical hazards only), worldwide |
| pdcSqs | alert area&central point, real-time info about current events, worldwide |
| pdcMapSrv | exposure area (polygons), worldwide (not for all severities of events) |
| gdacsAlert | xml with attributes, worldwide |
| gdacsAlertGeometry | json with more accurate geometry, worldwide |
| firms.modis-c6 | hotspots, <https://earthdata.nasa.gov/earth-observation-data/near-real-time/firms>, worldwide |
| firms.suomi-npp-viirs-c2 | hotspots, <https://earthdata.nasa.gov/earth-observation-data/near-real-time/firms>, worldwide |
| firms.noaa-20-viirs-c2 | hotspots, <https://earthdata.nasa.gov/earth-observation-data/near-real-time/firms>, worldwide |
| em-dat | hazards with info about loss, worldwide |
| storms.noaa | data for the US storms (tornadoes etc.) |
| tornado.canada-gov | static data for Canada tornado |
| tornado.australian-bm | static data for Australian tornado |
| tornado.osm-wiki | static data for tornado |
| tornado.des-inventar-sendai | static data for tornado |
| tornado.japan-ma | static data for Japan tornado |
| wildfire.nsw-gov | static data for Australian wildfires |
| wildfire.victoria-gov | static data for Australian wildfires |
| wildfire.qld-des-gov | static data for Australian wildfires |
| wildfire.sa-gov | static data for Australian wildfires |
| wildfire.frap.cal | static data for California wildfires |
| wildfire.calfire | real-time data for California wildfires |
| wildfire.perimeters.nifc | real-time data for US wildfires |
| wildfire.locations.nifc | real-time data for US wildfires |
| wildfire.inciweb | real-time data for US wildfires |

need to add TAOS and products(?) from pdc

### Output feeds

|     |     |     |
| --- | --- | --- |
| **Feed alias** | **Feed description** | **Provider(s)** |
| test-pdc-v0 | Pacific Disaster Center feed | hpSrvMag, hpSrvSearch, pdcSqs, pdcMapSrv |
| tets-gdacs | Global Disaster Alert and Coordination System feed | gdacsAlert, gdacsAlertGeometry |
| test-firms | Fire Information for Resource Management System | firms.modis-c6, firms.suomi-npp-viirs-c2, firms.noaa-20-viirs-c2 |
| test-em-dat | EM-DAT | em-dat |
| kontur-public | Public feed | gdacsAlert, gdacsAlertGeometry, firms.modis-c6, firms.suomi-npp-viirs-c2, firms.noaa-20-viirs-c2, kontur.events |
| test-inciweb | The feed contains real-time US wildfires as spots. | wildfire.inciweb |
| test-calfire | The feed contains real-time California wildfires as spots. Historical data starts from 2021. | wildfire.calfire |
| test-nifc | The feed contains real-time US wildfires as spots and perimeters. | wildfire.perimeters.nifc, wildfire.locations.nifc |
| test-cyclone | Real-time cyclones from NHC | cyclones.nhc-at.noaa, cyclones.nhc-ep.noaa, cyclones.nhc-cp.noaa |
| test-loss | Test feed for loss estimation | gdacsAlert, gdacsAlertGeometry, pdcMapSrv, pdcSqs |

