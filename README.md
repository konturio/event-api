[![codecov](https://codecov.io/gh/konturio/event-api/branch/main/graph/badge.svg)](https://app.codecov.io/gh/konturio/event-api)
## Profiles

Available profiles:
- For local use:
  - `jwtAuthDisabled` - Disables JWT authorization
  - `awsSqsDisabled` - Disables AWS SQS integration
- For external use:
  - `dev` - for DEV environment
  - `test` - for TEST environment
  - `prod` - for PROD environment

Profiles can be activated in several ways:
- using system parameter `-Dspring.profiles.active={profile_name}`
- using environment variable `export spring_profiles_active={profile_name}`

Several profiles can be activated at once with `,` separator: `profile1,profile2`

## Installation

#### Requirements

##### PostgreSQL extensions

 - PostGIS
 - uuid-ossp

#### Env properties

To configure additional external config file for Spring Boot application set up Env property:
`SPRING_CONFIG_ADDITIONAL_LOCATION="file:%h/config.local.yaml"`

#### Config file

There are five config files:
- `application.yml` - default config file
- `application-dev.yml` - config file for DEV environment
- `application-test.yml` - config file for TEST environment
- `application-prod.yml` - config file for PROD environment
- `config.yaml` - template of external config
- See `docs/feature_flags.md` for available feature flags.

We should use `dev`, `test` and `prod` config files to store different properties for different environments.

Secure data should be stored in an external config file: `config.local.yaml`

##### DB config

```yaml
spring:
  datasource:
    platform: postgres
    url: 'jdbc:postgresql://localhost:5432/db_name'
    username: USER_NAME
    password: 'CHANGE_ME_TO_SECURE_PASSWORD'
```

##### Authorization config

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: [ISSUER_URI]
          jwk-set-uri: [JWK_SET_URI]
```

- `ISSUER_URI` -  the base Keycloak Authorization Server URI. Like `http://[KEYCLOAK_HOST]/auth/realms/[REALM]/`
- `JWK_SET_URI` - `http://[KEYCLOAK_HOST]/auth/realms/[REALM]/protocol/openid-connect/certs`

##### Providers configuration

```yaml
pdc:
  host: 'https://testemops.pdc.org'
  user: user
  password: password
```

##### Job configuration

Jobs for data import:
- `hpSrvImport` - collects the raw data from PDC's Hazard and Product service (HpSrv)
- `gdacsImport` - collects the raw data from Gdacs
- ...

Jobs for data processing:
- `normalization` - normalizes the raw data
- `eventCombination` - combines episodes from normalized records into Kontur events
- `feedComposition` - creates customer feeds from normalized episodes
- `enrichment` - enriches events and episodes with analytics

```yaml
scheduler:
  hpSrvImport:
    enable: true
    initialDelay: 1000
  gdacsImport:
    enable: true
    cron: 0 1/5 * * * *
  normalization:
    enable: true
    initialDelay: 1000
    fixedDelay: 10000
  eventCombination:
    enable: true
    initialDelay: 10000
    fixedDelay: 10000
  feedComposition:
    enable: true
    initialDelay: 20000
    fixedDelay: 10000
  enrichment:
    enable: true
    initialDelay: 30000
    fixedDelay: 10000
```

#### Storing static data

AWS S3 bucket `event-api-locker01` is used for storing static data files.

- `PROD/` - for production
- `TEST QA/` - for testing
- `TEST DEV/` - for development
- `EXP/` - for experiments

#### Logging configuration

Logging to STDOUT should be switched off (level = OFF) for all tiers except local use.
For debugging could be used levels ERROR, WARN, INFO, DEBUG, TRACE
```xml
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>OFF</level>
        </filter>
        <encoder>
            <pattern>${defaultPattern}</pattern>
        </encoder>
    </appender>
```
