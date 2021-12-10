# Release notes

## UNRELEASED

#### Added

- metrics job for checking database status 
- add metrics to jobs subroutines that process one observation / event
- `wildfire.inciweb` provider
- added index `feed_data_enrichment_skipped_idx`
- DB tables autovacuum metrics
- added update date columns to tables: `normalized_at` - `normalized_observations`,
  `recombined_at` - `kontur_events`, `composed_at`, `enriched_at` - `feed_data`

#### Changed

- migrate to Java 16, update dependencies
- `em-dat` import job inherits from `AbstractJob` not `Runnuble`
- Nifc and Calfire combinators inherit from the new abstract class WildfireEpisodeCombinator  

#### Removed


## 0.9.1 - 2021-12-02

#### Changed

- Rename technical feeds, add `test` prefix: `test-gdacs`, `test-firms`, `test-em-dat`, `test-calfire`,
 `test-nifc`, `test-pdc-v0`


## 0.9 - 2021-12-01

#### Added

- post-processing for episodes in FeedCompositionJob
- `wildfire.calfire` provider
- `proper_name` and `region` for `wildfire.calfire` provider
- `wildfire.perimeters.nifc` and `wildfire.locations.nifc` providers

#### Changed

- moment episodes ascending sort


## 0.8.1 - 2021-11-16

#### Changed

- Fixed `WILDFIRE` type calculation during enrichment post-processing


## 0.8 - 2021-11-09

#### Added

- Added DEBUG logs to REST API
- Added `urls` column to `feed_data` and `urls` field to `FeedEpisode`
- Added `proper_name` column to `normalized_observations` and `feed_data` tables and `properName` field to `FeedEpisode`
- Added `location` column to `feed_data` and `location` field to `FeedEpisode`
- Added `enrichment_request` column to `feed_data`
- Added `populatedAreaKm2` enrichment field
- Fixed `'null'::jsonb` values from `event_details` column to be `null`
- Added migration to fill old events' `geometries` column in `feed_data`

#### Removed

- Removed `Cache-Control` header from response


## 0.7 - 2021-10-21

#### Added

- Added index on `feed_data` - `feed_data_updated_at_feed_id_is_not_enriched_idx`
- Separate firms' event combination, combine firms events iteratively
- **!!! BE AWARE !!!** Added `disaster-ninja-02` feed (for zigzag renamed `gdacs-firms` feed).
  We should change DN configuration in sync and add new role to Keycloak - `read:feed:disaster-ninja-02`
- Change `THERMAL_ANOMALY` type according to analytics, update name for events and episodes according to the type
- Added enrichment metrics
- Added `enrichment_attempts` column into `feed_data` table which indicates the number of attempts to enrich event
- Added feed composition metrics
- Added `enrichment_skipped` column into `feed_data` table which indicates whether event has been skipped during enrichment or not

#### Changed

- Async events enrichment execution
- Configs by profiles: `dev`, `test`, `prod`. We should leave only properties from
  `external/config.yaml` for external properties 
- Increased application heap memory size
- Parallel Firms Feed Composition execution


## 0.6.2 - 2021-08-30

#### Changed

- Fix empty event.observations in events/v1 endpoint


## 0.6.1 - 2021-08-27

#### Changed

- `geometries` filed in `feed_data` is calculated only for new events


## 0.6 - 2021-08-26

#### Added

- Analytic Enrichment step

```yaml
konturApps:
  host: 'https://apps.kontur.io/'

scheduler:
  enrichment:
    enable: true
    initialDelay: 30000
    fixedDelay: 10000
```

#### Changed

- Keycloak auth, we need to use ISSUER_URI as follows - `http://[KEYCLOAK_HOST]/auth/realms/[REALM]`,
  and JWK_SET_URI - `http://[KEYCLOAK_HOST]/auth/realms/[REALM]/protocol/openid-connect/certs`
- Added public feed
- Fixed GDACS job timing to prevent getting corrupted XML (job starts in 1 min after each time 
  GDACS loads CAP feed)

```yaml
security:
  oauth2:
    resourceserver:
      jwt:
        issuer-uri: https://keycloak01.kontur.io/auth/realms/kontur/
        jwk-set-uri: https://keycloak01.kontur.io/auth/realms/kontur/protocol/openid-connect/certs
scheduler:
  gdacsImport:
    cron: 0 1/5 * * * *
feedComposition:
    alias: gdacs, em-dat, pdc-v0, swissre-02, kontur-public
```

#### Removed

```yaml
auth0:
  audience: 'https://apps.kontur.io/events/'
```


## 0.5 - 2021-07-22

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

konturApps:
  host: 'https://test-apps02.konturlabs.com/'
  
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
