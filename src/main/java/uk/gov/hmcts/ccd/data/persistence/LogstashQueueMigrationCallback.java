package uk.gov.hmcts.ccd.data.persistence;

import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.callback.BaseCallback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.sql.Statement;

/** Preserves the published migration checksum while allowing its backlog rewrite and DDL. */
@Component
public class LogstashQueueMigrationCallback extends BaseCallback {
    @Override
    public boolean supports(Event event, Context context) {
        return event == Event.BEFORE_EACH_MIGRATE
            && context.getMigrationInfo() != null
            && MigrationVersion.fromVersion("20260918.0000").equals(context.getMigrationInfo().getVersion());
    }

    @Override
    public void handle(Event event, Context context) {
        // Reinserted backlog rows reference existing cases. Check their FK immediately so
        // pending deferred checks cannot block ADD CONSTRAINT later in this transaction.
        // The schema's INITIALLY DEFERRED default is unchanged for normal case creation.
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("SET CONSTRAINTS public.case_data_logstash_queue_case_data_id_fk IMMEDIATE");
        } catch (SQLException exception) {
            throw new FlywayException("Cannot prepare Logstash queue coalescing migration", exception);
        }
    }
}
