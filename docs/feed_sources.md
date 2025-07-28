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
| `usgs.earthquake` | Earthquake events from the USGS 4.5‑week feed |

## usgs.earthquake ##

For USGS earthquakes `geometries` may contain ShakeMap polygons. They are derived
from contour lines published by USGS. If `shakemap` is an array, only
its first element is used. ShakeMap polygons do not include the original
`Class`, `country` or `areaType` attributes. Polygons whose `value` ends up
`null` are discarded during normalization. If `cont_mmi` contains no
`features` array, the polygons are skipped. Each polygon is enriched with
`Class`, `eventid`, `eventtype` and `polygonlabel` derived from the intensity
value. `Class` becomes `Poly_SMPInt_&lt;intensity&gt;`, `eventid` matches the
earthquake external ID, `eventtype` is `EQ` and `polygonlabel` is
`Intensity &lt;intensity&gt;`. If the numeric `maxpga` value in ShakeMap
properties reaches at least `0.4` and the data provides `coverage_pga_high_res`,
a union of pixels with PGA above `0.4 g` is computed and stored as a GeoJSON
object in `severity_data` under the key `pga40Mask`.
If ShakeMap provides `coverage_pga_high_res`, it is copied to `severity_data` under `coverage_pga_highres`.
Polygons created from ShakeMap contours and the `pga40Mask` are shifted with `ST_ShiftLongitude` if they cross the antimeridian so that longitudes stay within `[-180, 180]`.
For every USGS earthquake a circular polygon with 100&nbsp;km radius is built around the epicenter. If this buffer crosses the antimeridian it is also shifted.
It is stored in `geometries` with properties `Class`=`Poly_Circle`, `eventid` equal
to the external ID, `areaType`=`alertArea`, `eventtype`=`EQ` and `polygonlabel`=`100km`.