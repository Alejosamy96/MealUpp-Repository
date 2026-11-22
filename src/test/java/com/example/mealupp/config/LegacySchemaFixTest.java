package com.example.mealupp.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LegacySchemaFixTest {

  @Mock private DataSource dataSource;
  @Mock private Connection connection;
  @Mock private Statement statement;

  @Test
  void dropsTheOutdatedStatusCheck() throws SQLException {
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);

    LegacySchemaFix.dropOutdatedStatusCheck(dataSource);

    verify(statement).execute(LegacySchemaFix.DROP_STATUS_CHECK);
  }

  @Test
  void aDatabaseErrorDoesNotStopTheApplication() throws SQLException {
    when(dataSource.getConnection()).thenThrow(new SQLException("sin conexión"));

    assertThatCode(() -> LegacySchemaFix.dropOutdatedStatusCheck(dataSource))
        .doesNotThrowAnyException();
  }
}
