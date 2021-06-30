# Release notes

## UNRELEASED
#### Added

- tornado.japan-ma provider (currently disabled)
- pdcMapSrv
- Change time of jobs delay: normalization, eventCombination, feedComposition - from 60000 to 1000 ms.
  Added firms feed composition job. Added alias values to feedComposition.

```yaml

pdc:
  mapSrvHost: 'https://testapps.pdc.org'
tornadoJapanMa:
  host: 'https://www.data.jma.go.jp/obd/stats/data/bosai/tornado/'

scheduler:
  pdcMapSrvSearch:
    enable: true
    initialDelay: 1000
    fixedDelay: 60000
  tornadoJapanMaImport:
    enable: false
    initialDelay: 1000
    fixedDelay: P1D
  historicalTornadoJapanMaImport:
    enable: false
    initialDelay: 1000
  normalization:
    fixedDelay: 1000
  eventCombination:
    fixedDelay: 1000
  feedComposition:
    fixedDelay: 1000
    alias: gdacs, em-dat, pdc-v0, swissre-02
  firmsFeedComposition:
    enable: true
    initialDelay: 1000
    fixedDelay: 1000
    alias: firms
```

#### Removed

```yaml
feign:
  client:
    config:
      pdcHpSrvBasicAuth:
        requestInterceptors:
          - feign.auth.BasicAuthRequestInterceptor
```

#### Changed
- !BE AWARE! It might take a  while to install this version. Heavy sql scripts will be run.  
- Denormalize feed_data table in order to improve search speed. #4736 #5183 #5168 
- Static data files stored in AWS S3 bucket `event-api-locker01`. The following folders are used: 
  `PROD/` for production, `TEST QA/` for testing, `TEST DEV/` for development, `EXP/` for experiments. Default 
  folder is `PROD/`. Folder can be changed in configuration.

## 0.4.1 - 2021-04-30
#### Added
- GDACS Wildfire type recognition

## 0.4 - 2021-04-23
#### Added
- `bbox` and `datetime` filter to the `/v1/` endpoint
- FIRMS provider
- EM-DAT provider
- Static data providers 
  - tornado.canada-gov 
  - tornado.australian-bm
  - tornado.osm-wiki
  - wildfire.frap.cal 
  - tornado.des-inventar-sendai
  - wildfire.sa-gov
  - wildfire.qld-des-gov
  - wildfire.victoria-gov
  - wildfire.nsw-gov
- storms.noaa provider

#### Changed
- Event versioning is removed 
- Refactored normalization, event and episods jobs

### Configuration changes
#### Added
```yaml
emdat:
  user: 'username'
  password: 'password'
  host: 'https://public.emdat.be/api'

konturApi:
  host: 'https://api.kontur.io/'

stormsNoaa:
  host: 'https://www1.ncdc.noaa.gov/pub/data/swdi/stormevents/csvfiles/'

staticdata:
  s3Bucket: 'event-api-locker01'
  s3Folder: 'PROD/'

scheduler:
  firmsImport:
    enable: true
    initialDelay: 1000
    fixedDelay: 3600000
  emDatImport:
    enable: true
    initialDelay: 1000
    fixedDelay: PT3H # every 3 hours
  staticImport:
    enable: true
    initialDelay: 1000
  stormsNoaaImport:
    enable: true
    initialDelay: 1000
    fixedDelay: P30D # every 30 days
```

## 0.3 - 2020-11-18
#### Added
- GDACS provider
- `/v1/event` endpoint
- `types` and `severities` filter to the `/v1/` endpoint
#### Changed
- Changed `/v1/` endpoint pagination to cursor based.

### Configuration changes
#### Added
```yaml
gdacs:
  host: 'https://www.gdacs.org'

scheduler:
  hpSrvMagsImport:
    enable: true
    initialDelay: 10000
    fixedDelay: 600000
  gdacsImport:
    enable: true
    cron: 30 0/5 * * * *
```
