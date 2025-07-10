# Event Feed Sources

The system ingests data from multiple providers. Each provider name reflects its origin or the organization maintaining the dataset.

| Provider ID | Description |
| ----------- | ----------- |
| `hpSrvSearch` | Hazard search API of the Pacific Disaster Center |
| `hpSrvMag` | Exposure information from PDC Hazard service |
| `pdcSqs` | Real-time messages from PDC via AWS SQS |
| `pdcMapSrv` | Exposure layers from PDC Map Service |
| `pdcSqsNasa` | NASA related SQS messages from PDC |
| `pdcMapSrvNasa` | NASA related layers from PDC Map Service |
| `gdacsAlert` | Alerts from the Global Disaster Alert and Coordination System |
| `gdacsAlertGeometry` | Geometry data for GDACS alerts |
| `em-dat` | Historical events from the EM-DAT database |
| `kontur.events` | Humanitarian crisis events maintained by Kontur |
| `firms.modis-c6` | Wildfire detections from NASA MODIS |
| `firms.suomi-npp-viirs-c2` | Wildfire detections from NASA Suomi NPP VIIRS |
| `firms.noaa-20-viirs-c2` | Wildfire detections from NOAA-20 VIIRS |
| `wildfire.calfire` | California wildfire feed |
| `wildfire.inciweb` | InciWeb incidents feed |
| `wildfire.perimeters.nifc` | Wildfire perimeters from NIFC |
| `wildfire.locations.nifc` | Wildfire locations from NIFC |
| `wildfire.frap.cal` | California FRAP wildfire history |
| `wildfire.sa-gov` | Wildfire reports from South Australia government |
| `wildfire.qld-des-gov` | Queensland Department of Environment data |
| `wildfire.victoria-gov` | Victoria State wildfire feed |
| `wildfire.nsw-gov` | New South Wales wildfire feed |
| `tornado.canada-gov` | Tornado history from Government of Canada |
| `tornado.australian-bm` | Tornado reports from Australian Bureau of Meteorology |
| `tornado.osm-wiki` | Historical tornado data from OpenStreetMap wiki |
| `tornado.des-inventar-sendai` | Disaster inventory data from DesInventar Sendai |
| `tornado.japan-ma` | Tornado cases from the Japan Meteorological Agency |
| `storms.noaa` | NOAA Storm Events Database |
| `cyclones.nhc-at.noaa` | Atlantic cyclone advisories from NHC |
| `cyclones.nhc-cp.noaa` | Central Pacific cyclone advisories from NHC |
| `cyclones.nhc-ep.noaa` | Eastern Pacific cyclone advisories from NHC |
| `usgs.earthquake` | Earthquake events from the USGS hourly feed with optional ShakeMap and loss estimation enrichment |

ShakeMap data is fetched from the event detail feed when available. If the file
`cont_pga.json` exists, its content is stored along with the event and the
download link is placed in the `shm_url` field. The `coverage_pga_high_res.covjson`
file is also downloaded and embedded when present. If either download fails,
the `shakemap_cont_retrieval` or `shakemap_hishres_pga_retrieval` flag is set to
`false` respectively. If the detail entry also contains loss estimation information
(`losspager` product), the `loss_url` field stores a link to `losses.json` and the
parsed data is saved under `loss_estimation`.
