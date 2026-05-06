package com.jober.searchservice.integration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

public abstract class PostgresContainerBaseIT {

  @DynamicPropertySource
  static void configureDataSource(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.datasource.url",
        () ->
            System.getProperty(
                "search.it.db.url", "jdbc:postgresql://localhost:55432/search_service_it"));
    registry.add("spring.datasource.username", () -> System.getProperty("search.it.db.user", "it_user"));
    registry.add("spring.datasource.password", () -> System.getProperty("search.it.db.pass", "it_pass"));
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    registry.add("spring.flyway.enabled", () -> "false");
    registry.add(
        "spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation", () -> "true");
    registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQL10Dialect");
  }
}
