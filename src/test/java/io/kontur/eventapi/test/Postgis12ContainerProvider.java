package io.kontur.eventapi.test;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.JdbcDatabaseContainerProvider;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.jdbc.ConnectionUrl;

public class Postgis12ContainerProvider extends JdbcDatabaseContainerProvider {

  @Override
  public boolean supports(String databaseType) {
    return databaseType.equals("postgis12");
  }

  @Override
  public JdbcDatabaseContainer<?> newInstance() {
    return newInstance("");
  }

  @Override
  public JdbcDatabaseContainer<?> newInstance(String tag) {
    return new PostgreSQLContainer<>("postgis/postgis:12-3.0-alpine");
  }

  @Override
  public JdbcDatabaseContainer<?> newInstance(ConnectionUrl connectionUrl) {
    return newInstanceFromConnectionUrl(connectionUrl, "username", "password");
  }
}