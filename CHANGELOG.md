# Release notes

## Unreleased

#### Added
- FIRMS provider
#### Changed
- Event versioning is removed 
- Refactored normalization, event and episods jobs

### Configuration changes
#### Added
```yaml
scheduler:
  firmsImport:
    enable: true
    initialDelay: 1000
    fixedRate: 3600000
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