## Profiles

Available profiles:
- `develop` - Disables JWT authorization
- `awsSqsDisabled` - Disables AWS SQS integration

Profiles can be activated several ways:
- using system parameter `-Dspring.profiles.active={profile_name}`
- using environment variable `export spring_profiles_active={profile_name}`

Several profiles can be activated at once via `,` separator: `profile1,profile2`

## Installation

#### Requirements
#####Postgresql extensions 
 - Postgis
 - uuid-ossp
 
#### Env properties

To configure additional external config file for Spring Boot application set up Env property: 
`SPRING_CONFIG_ADDITIONAL_LOCATION="file:%h/config.local.yaml"`

#### Config file

#####DB config

```yaml
spring:
  datasource:
    platform: postgres
    url: 'jdbc:postgresql://localhost:5432/db_name'
    username: USER_NAME
    password: 'CHANGE_ME_TO_SECURE_PASSWORD'
```

#####Authorization config

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

#####Providers configuration

```yaml
pdc:
  host: 'https://testemops.pdc.org'
  user: user
  password: password
```

##### Job configuration

There are 4 jobs collecting the data.
- `hpSrvImport` - collects the raw data from PDC's Hazard and Product service (HpSrv); 
- `gdacsImport` - collects the raw data from Gdacs; 
- `normalization` - normalizes the raw data;
- `eventCombination` - combines episodes from normalized records into Kontur events;
- `feedComposition` - creates customer feeds from normalized episodes.

```yaml
scheduler:
  hpSrvImport:
    enable: true
    initialDelay: 1000
  gdacsImport:
    enable: true
    initialDelay: 1000
    fixedRate: 300000
  normalization:
    enable: true
    initialDelay: 1000
    fixedDelay: 60000
  eventCombination:
    enable: true
    initialDelay: 10000
    fixedDelay: 60000
  feedComposition:
    enable: true
    initialDelay: 20000
    fixedDelay: 60000
```

#### Storing static data

AWS S3 bucket `event-api-locker01` is used for storing static data files.

- `PROD/` - for production
- `TEST QA/` - for testing
- `TEST DEV/` - for development
- `EXP/` - for experiments 
