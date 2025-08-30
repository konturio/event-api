# Running tests

All unit and integration tests run with Maven by default:

```bash
mvn test
```

Most tests spin up PostgreSQL and Redis containers using Testcontainers.
They run by default together with unit tests.
If Docker isn't available you can skip them:

```bash
mvn -DskipITs=true test
```

Unit tests always run regardless of Docker availability.

Maven honours `HTTP_PROXY` and `HTTPS_PROXY` variables.
The Makefile sets JVM options (`-Djava.net.useSystemProxies=true`, `-Djava.net.preferIPv4Stack=true`, `-Djava.net.preferIPv6Addresses=false`) so Maven respects those proxies without a `.mvn/jvm.config` file.
`make` targets append proxy command-line arguments when these variables are set, avoiding global configuration changes.
