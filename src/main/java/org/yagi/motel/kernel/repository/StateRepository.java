package org.yagi.motel.kernel.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Optional;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.yagi.motel.kernel.model.dsl.Tables;
import org.yagi.motel.kernel.model.enums.IsProcessedState;

@SuppressWarnings("checkstyle:MissingJavadocType")
public class StateRepository {
  private static final byte STATE_RECORD_ID = 1;
  private final DSLContext dsl;

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public StateRepository(String dbFilename) throws SQLException {
    this.dsl = DSL.using(getConnection(dbFilename), SQLDialect.SQLITE);
    prepareDb(this.dsl);
  }

  private Connection getConnection(String dbFilename) throws SQLException {
    Connection connection =
        DriverManager.getConnection(String.format("jdbc:sqlite:%s", dbFilename));
    return connection;
  }

  private void prepareDb(DSLContext dsl) {
    dsl.execute("PRAGMA encoding = \"UTF-8\";");
    dsl.execute("PRAGMA synchronous=FULL;");
    dsl.execute(
        "CREATE TABLE IF NOT EXISTS state (id tinyint PRIMARY KEY, is_processed_enable tinyint);");
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public void updateProcessingState(IsProcessedState processedState) {
    dsl.transaction(
        ctx -> {
          byte state = Integer.valueOf(processedState.getState()).byteValue();
          DSL.using(ctx)
              .insertInto(Tables.STATE, Tables.STATE.ID, Tables.STATE.IS_PROCESSED_ENABLE)
              .values(STATE_RECORD_ID, state)
              .onDuplicateKeyUpdate()
              .set(Tables.STATE.IS_PROCESSED_ENABLE, state)
              .execute();
        });
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public Optional<IsProcessedState> getIsProcessedState() {
    Record result =
        dsl.select().from(Tables.STATE).where(Tables.STATE.ID.eq(STATE_RECORD_ID)).fetchOne();
    if (result != null) {
      byte state = result.get(Tables.STATE.IS_PROCESSED_ENABLE);
      return IsProcessedState.resolveByValue(Byte.valueOf(state).intValue());
    }
    return Optional.empty();
  }
}
