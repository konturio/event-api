# Release notes

## Unreleased
#### Added
- `bbox` and `datetime` filter to the `/v1/` endpoint
- FIRMS provider
- EM-DAT provider
- Static data providers (tornado.canada-gov, tornado.australian-bm, tornado.osm-wiki, wildfire.frap.cal, tornado.des-inventar-sendai)
- Noaa provider
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

noaaTornado:
  host: 'https://www1.ncdc.noaa.gov/pub/data/swdi/stormevents/csvfiles/'

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
  noaaTornadoImport:
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