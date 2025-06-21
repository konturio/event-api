# Separating REST API from ETL Pipeline

The application ships with multiple scheduled jobs responsible for data import,
normalization and enrichment. These jobs run by default whenever the application
starts and are part of the ETL pipeline.

To run the REST API without starting the ETL pipeline set the `pipelineDisabled`
Spring profile. When this profile is active the scheduler beans are not created
and no jobs will be executed.

Example:

```bash
SPRING_PROFILES_ACTIVE=pipelineDisabled,dev ./run.sh
```

This allows deploying the API service separately from the pipeline if needed.

