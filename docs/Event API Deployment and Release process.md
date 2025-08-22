# Event API Deployment and Release process

Field: Content

## Environments

There are 3 stages used for the Event API project:
* **Dev** - for developers 
* **Test** - for QA's
* **Prod** - for production

## How to deploy Event API?

General detailed instructions: [[Tasks/document: Deployment and release process for Java applications and Layers DB ETL#^b2d59af0-3b70-11e9-be77-04d77e8d50cb/82759010-550a-11ed-8d36-f3567567770c]] 

### Dev

1. Prepare PR in the [disaster-ninja-cd](https://github.com/konturio/disaster-ninja-cd "https://github.com/konturio/disaster-ninja-cd") repository
   1. Update the `image → tag` property to the tag of the branch you want to deploy in [values-dev.yaml](https://github.com/konturio/disaster-ninja-cd/blob/main/helm/event-api/values/values-dev.yaml "https://github.com/konturio/disaster-ninja-cd/blob/main/helm/event-api/values/values-dev.yaml")
   2. Update `version` in [Chart.yaml](https://github.com/konturio/disaster-ninja-cd/blob/main/helm/event-api/Chart.yaml "https://github.com/konturio/disaster-ninja-cd/blob/main/helm/event-api/Chart.yaml") (`0.1.10` → `0.1.11`)
   3. *Optional:* update configs [values-dev.yaml](https://github.com/konturio/disaster-ninja-cd/blob/main/helm/event-api/values/values-dev.yaml "https://github.com/konturio/disaster-ninja-cd/blob/main/helm/event-api/values/values-dev.yaml") (be careful updating general [values.yaml](https://github.com/konturio/disaster-ninja-cd/blob/main/helm/event-api/values.yaml "https://github.com/konturio/disaster-ninja-cd/blob/main/helm/event-api/values.yaml"), it will cause the deployment to all stages)
   4. Send the PR to review, and wait for it to be reviewed
2. Write to slack `#01_test_tier_changes` about deployment
3. *Optional:* Migrations
   1. Generate migrations update script
   2. Run migrations on DEV DB while the current Event API version is running
4. Deploy - merge PR in the [disaster-ninja-cd](https://github.com/konturio/disaster-ninja-cd "https://github.com/konturio/disaster-ninja-cd") repository.

### Test

1. Prepare PR in the [disaster-ninja-cd](https://github.com/konturio/disaster-ninja-cd "https://github.com/konturio/disaster-ninja-cd") repository
   1. Update the `image → tag` property to the tag of the branch you want to deploy in [values-test.yaml](https://github.com/konturio/disaster-ninja-cd/blob/main/helm/event-api/values/values-test.yaml "https://github.com/konturio/disaster-ninja-cd/blob/main/helm/event-api/values/values-test.yaml")
   2. Update `version` in [Chart.yaml](https://github.com/konturio/disaster-ninja-cd/blob/main/helm/event-api/Chart.yaml "https://github.com/konturio/disaster-ninja-cd/blob/main/helm/event-api/Chart.yaml") (`0.1.10` → `0.1.11`)
   3. *Optional:* update configs [values-test.yaml](https://github.com/konturio/disaster-ninja-cd/blob/main/helm/event-api/values/values-test.yaml "https://github.com/konturio/disaster-ninja-cd/blob/main/helm/event-api/values/values-test.yaml") (be careful updating general [values.yaml](https://github.com/konturio/disaster-ninja-cd/blob/main/helm/event-api/values.yaml "https://github.com/konturio/disaster-ninja-cd/blob/main/helm/event-api/values.yaml"), it will cause the deployment to all stages)
   4. Send the PR to review, and wait for it to be reviewed
2. Write to slack `#01_test_tier_changes` about deployment
3. *Optional:* Migrations
   1. Generate migrations update script
   2. Run migrations on TEST DB while the current Event API version is running
4. Deploy - merge PR in the [disaster-ninja-cd](https://github.com/konturio/disaster-ninja-cd "https://github.com/konturio/disaster-ninja-cd") repository.

### Prod 

1. Create a release branch. The release branch should have a name: `release-[VERSION]` (ex. release-1.12)
   1. Rename the version in the `pom.xml` file (ex. `1.11-SNAPSHOT` → `1.12-SNAPSHOT`)
   2. Merge the Event API release branch into the master (without deleting the branch)
2. Generate release notes in GitHub. 
3. *Send release notes to the channel `#release-notes`*
4. Prepare PR in the [disaster-ninja-cd](https://github.com/konturio/disaster-ninja-cd "https://github.com/konturio/disaster-ninja-cd") repository
   1. Update the `image → tag` property to the tag of the release branch you want to deploy in [values-prod.yaml](https://github.com/konturio/disaster-ninja-cd/blob/main/helm/event-api/values/values-prod.yaml "https://github.com/konturio/disaster-ninja-cd/blob/main/helm/event-api/values/values-prod.yaml")
   2. Update `version` in [Chart.yaml](https://github.com/konturio/disaster-ninja-cd/blob/main/helm/event-api/Chart.yaml "https://github.com/konturio/disaster-ninja-cd/blob/main/helm/event-api/Chart.yaml") (`0.1.10` → `0.1.11`)
   3. *Optional:* update configs [values-prod.yaml](https://github.com/konturio/disaster-ninja-cd/blob/main/helm/event-api/values/values-prod.yaml "https://github.com/konturio/disaster-ninja-cd/blob/main/helm/event-api/values/values-prod.yaml") (be careful updating general [values.yaml](https://github.com/konturio/disaster-ninja-cd/blob/main/helm/event-api/values.yaml "https://github.com/konturio/disaster-ninja-cd/blob/main/helm/event-api/values.yaml"), it will cause the deployment to all stages)
   4. Send the PR to review, and wait for it to be reviewed 
5. Write to slack `#02_prod_tier_changes` about the release
6. *Optional:* Migrations
   1. Generate migrations update script
   2. Create manual prod DB backup (delete the manual backup from the previous release)
   3. Run migrations on PROD DB while the current Event API version is running
7. Deploy - merge PR in the [disaster-ninja-cd](https://github.com/konturio/disaster-ninja-cd "https://github.com/konturio/disaster-ninja-cd") repository.

## Configuration

Event API has a multilevel configuration. Config files are considered in the following order:

1. Application properties packaged inside the JAR ([application.yml](https://gitlab.com/kontur-private/platform/event-api/-/blob/master/src/main/resources/application.yml "https://gitlab.com/kontur-private/platform/event-api/-/blob/master/src/main/resources/application.yml")) - setup for local run
2. Profile-specific applications properties packaged inside the JAR ([application-dev.yml](https://github.com/konturio/event-api/blob/master/src/main/resources/application-dev.yml "https://github.com/konturio/event-api/blob/master/src/main/resources/application-dev.yml"), [application-test.yml](https://github.com/konturio/event-api/blob/master/src/main/resources/application-test.yml "https://github.com/konturio/event-api/blob/master/src/main/resources/application-test.yml"), [application-prod.yml](https://github.com/konturio/event-api/blob/master/src/main/resources/application-prod.yml "https://github.com/konturio/event-api/blob/master/src/main/resources/application-prod.yml"))
3. Application properties outside of the packaged JAR ([disaster-ninja-cd](https://github.com/konturio/disaster-ninja-cd/tree/main/helm/event-api/values "https://github.com/konturio/disaster-ninja-cd/tree/main/helm/event-api/values"))
4. Profile-specific application properties packaged outside the JAR ([values-dev.yml](https://gitlab.com/kontur-private/platform/event-api/-/blob/master/src/main/resources/application-dev.yml "https://gitlab.com/kontur-private/platform/event-api/-/blob/master/src/main/resources/application-dev.yml"), [values-test.yml](https://gitlab.com/kontur-private/platform/event-api/-/blob/master/src/main/resources/application-test.yml "https://gitlab.com/kontur-private/platform/event-api/-/blob/master/src/main/resources/application-test.yml"), [values-prod.yml](https://gitlab.com/kontur-private/platform/event-api/-/blob/master/src/main/resources/application-prod.yml "https://gitlab.com/kontur-private/platform/event-api/-/blob/master/src/main/resources/application-prod.yml")) 

The file [application.yml](https://gitlab.com/kontur-private/platform/event-api/-/blob/master/src/main/resources/application.yml "https://gitlab.com/kontur-private/platform/event-api/-/blob/master/src/main/resources/application.yml") contains all the properties required for the application and provides the default values for them. 

The files [values-dev.yml](https://gitlab.com/kontur-private/platform/event-api/-/blob/master/src/main/resources/application-dev.yml "https://gitlab.com/kontur-private/platform/event-api/-/blob/master/src/main/resources/application-dev.yml"), [values-test.yml](https://gitlab.com/kontur-private/platform/event-api/-/blob/master/src/main/resources/application-test.yml "https://gitlab.com/kontur-private/platform/event-api/-/blob/master/src/main/resources/application-test.yml"), [values-prod.yml](https://gitlab.com/kontur-private/platform/event-api/-/blob/master/src/main/resources/application-prod.yml "https://gitlab.com/kontur-private/platform/event-api/-/blob/master/src/main/resources/application-prod.yml") rewrite the properties that have specific values for the **dev**, **test**, or **prod** environment. 

Passwords and other credentials are stored as secrets and can be accessed through, for example, Lens.

## Database migrations

We have to follow the SLA: [[Tasks/Task: Create a doc with major requirements to Event API performance from the SLA#^7b708802-3c0b-11e9-9428-04d77e8d50cb/f2fd3030-1222-11ec-b406-9b550724b411]] 

### General rules for making database migrations in the Event API:
* Migrations have to be compatible with the old and new application versions
* Migrations shouldn't block tables' READ and WRITE operations or make it with the least delay
* REST API should work during migrations because other applications depend on it
* Data import should work during migrations, so we don't lose data
* Always add migrations' rollback scripts
* *If there is no way to make the migration faster, we can improve DB performance by temporarily changing its settings*

### Database migration process:

1. Generate Liquibase update script
2. *Optional (Only for prod):* Run the prod DB snapshot and test the update script on it to ensure the execution time and see if it works fine
3. Run the migrations while the current version of the application is running
4. Deploy the new version of the application

### How to generate the Liquibase update script?

1. Install Liquibase command-line tool ([link](https://docs.liquibase.com/concepts/installation/installation-linux-unix-mac.html "https://docs.liquibase.com/concepts/installation/installation-linux-unix-mac.html"), check the version that Event API is using - currently 4.5.0)
2. Download the PostgreSQL JDBC [driver](https://jdbc.postgresql.org/download.html "https://jdbc.postgresql.org/download.html") and put it into the liquibase `lib` folder \
   (for mac `/usr/local/opt/liquibase/libexec/lib`, for windows `C:\Program Files\liquibase\lib`)
3. For convenience redirect the DB connection to your localhost
4. Run the following command from the `resources` folder of the Event API application:

```
Linux:

liquibase --changeLogFile=/db/changelog/db.changelog-master.yaml --url=jdbc:postgresql://localhost:15432/event-api --username=event-api --password=[DB_PASSWORD] --outputFile=[PATH_TO_OUTPUT_FILE] updateSQL
```

```
Windows:

liquibase --changelog-file=db\changelog\db.changelog-master.yaml --url=jdbc:postgresql://localhost:15432/event-api  --password="[DB_PASSWORD]" --username=event-api --outputFile=migration.sql updateSQL
```

The update script will be saved in the file specified in the `[PATH_TO_OUTPUT_FILE]` option.

### Useful resources on making migrations:
* [Evolutionary Database Design](https://martinfowler.com/articles/evodb.html "https://martinfowler.com/articles/evodb.html")
* [Fast Column Creation with Defaults](https://brandur.org/postgres-default "https://brandur.org/postgres-default")
* [Adding NOT NULL constraints for large tables](https://habr.com/ru/company/haulmont/blog/493954/ "https://habr.com/ru/company/haulmont/blog/493954/")

## GitHub 

### Branch / MR / Commit naming
* **Branch** 
  * Take from the Fibery task `BRANCH NAME`
  * or custom with the following format
    * `{task number}-some-text` for a task, ex. *1234*-do-work
    * `us{us number}-some-text` for a user story, ex. *us123-do-work*
* **MR**
  * `{task number} some text` for a task, ex. *1234 Do work*
  * `US{us number} some text` for a user story, ex. *US123 Do work*
* **Commit**
  * `#{task number} some text` for one task, ex. *#1234 do work*
  * `#{task number} #{another task number} some text` for multiple tasks, ex. *#1234 #1235 do work*

## Versioning

[Semver](https://semver.org/ "https://semver.org/") is used as a base for this method

### Version number structure

`MAJOR.MINOR.PATCH` → ex. `0.10.0`, `0.12.1`, `1.0.0`

*Major = 0* - for initial development, anything may change at any time, the public API should not be considered stable.

*Major = 1* - for the public API 

|     |     |     |
| --- | --- | --- |
| **Increment the major version (major > 0)** | **Increment minor version** | **Increment patch version** |
| Incompatible changes to the API Incompatible changes to the data structure returned by the API *Ex. Remove endpoint, remove request parameter (filter), change response structure, remove response field* | Changes to the internal structure of the application Compatible changes to the API *Ex. Add new endpoint, add new parameter (filter), add  response field* | Bug fixes (hot-fixes) |
* The API version changes with the MAJOR version.**
