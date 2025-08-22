# Data sources errors

*This document was created in order to specify errors in data sources, to type them.*

*Also, to store files with a list of errors to provide the source with an indication of their status, track the status of fixes.*

Statuses list <TBD>
* errors are collected
* sent to the data source (+sending date)
* the answer is received (fixed / not fixed)

|     |     |     |     |     |
| --- | --- | --- | --- | --- |
| **Data source** | **Error description** | **\~ Errors qty** | **Attached file name** | **STATUS** |
| [storms.noaa](https://www.ncdc.noaa.gov/stormevents/ftp.jsp "https://www.ncdc.noaa.gov/stormevents/ftp.jsp") (mail: ncei.orders@noaa.gov) | WRONG COORDINATES too long tornado's lines, missing minus ("-") | 7 | noaa_potential_errors.xlsx | 2021-04-08 sent to data source 2021-11-24 partly fixed - 1999y is fixed, 1997 is not fixed |
|  | 2006 year missing | \- | \- | 2021-04-28 sent to data source  2021-04-29 answer is received: "We are working to restore the data from 2006." 2021-11-24 fixed - 2006 year exists, already in db  |
|  | Time zones | 169 | Time_zones_errors.xlsx | 2021-04-08 sent to data source 2021-11-24 not fixed |
| [wildfire.frap.cal](https://frap.fire.ca.gov/frap-projects/fire-perimeters/ "https://frap.fire.ca.gov/frap-projects/fire-perimeters/") | invalid year (as 2109) | 5 | frap_potential_errors.xlsx | errors are collected |
|  | alarm date is later than containment date | 26 | errors are collected |
|  | year (separate column) doesn't match alarm date and containment date | 20 | errors are collected |
|  | long \[31;100) days or too long \[100; +∞) days fires | 501; 162 | errors are collected |
| [gdacs](https://www.gdacs.org/ "https://www.gdacs.org/") | cancelled alerts (presumably are deleted in initial DB) | 1 (for now) | <https://disaster.ninja/live/#id=2d0c40b1-9f0b-4cd1-9015-ce2a2c2effdd;zoom=1.7589696079034485;position=47.95041457675171,-13.784853765486332>  |  |
| [em-dat](https://www.emdat.be/ "https://www.emdat.be/") | WRONG COORDINATES (too big or too small) | 67  | em-dat_potential_errors.xlsx | errors are collected |
| [CalFire](https://www.fire.ca.gov/ "https://www.fire.ca.gov/") (mail: webapp@readyforwildfire.org) | ended_at is earliest than started_at | 4 (2021) 48 (total) | link: <https://docs.google.com/spreadsheets/d/1mBDuLZ-rQlm1zAX79SX1tlxDDs6PXFauf-RPQ7vjXTk/edit?usp=sharing> | 2021-11-25 sent to data source  |

