package com.example.mealupp.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Arregla bases de datos creadas antes de que existieran estados nuevos (por ejemplo READY).
 *
 * <p>Hibernate crea una regla CHECK con los estados que existían al crear la tabla y, con
 * ddl-auto=update, no la actualiza cuando se agrega un estado. Por eso, en una base vieja, pasar un
 * pedido a READY falla. Aquí se borra esa regla al arrancar; en una base nueva no hace falta y no
 * pasa nada si no existe.
 */
@Configuration
@Profile("!test")
public class LegacySchemaFix {

  private static final Logger LOG = LoggerFactory.getLogger(LegacySchemaFix.class);

  static final String DROP_STATUS_CHECK =
      "ALTER TABLE customer_orders DROP CONSTRAINT IF EXISTS customer_orders_status_check";

  @Bean
  public CommandLineRunner outdatedStatusCheckRunner(DataSource dataSource) {
    return args -> dropOutdatedStatusCheck(dataSource);
  }

  /** Borra la regla vieja de estados. Si falla, se avisa en el log pero la aplicación arranca. */
  static void dropOutdatedStatusCheck(DataSource dataSource) {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute(DROP_STATUS_CHECK);
    } catch (SQLException ex) {
      LOG.warn("No se pudo revisar la regla de estados de customer_orders: {}", ex.getMessage());
    }
  }
}
