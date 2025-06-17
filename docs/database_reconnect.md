# Database Reconnect Job

The API uses HikariCP for database connections. In rare cases the pool may hold
broken connections which block queries. A scheduled job `DatabaseReconnectJob`
checks the datasource every minute.

If the job fails to obtain a connection it evicts all connections from the pool.
This forces HikariCP to open new ones automatically. Scheduler parameters can be
changed in `application.yml` under `scheduler.dbReconnect`.
