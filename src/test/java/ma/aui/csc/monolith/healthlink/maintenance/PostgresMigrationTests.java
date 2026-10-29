package ma.aui.csc.monolith.healthlink.maintenance;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import ma.aui.csc.monolith.healthlink.HealthLinkApplication;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import static org.assertj.core.api.Assertions.*;

/** Opt-in tests only accept a disposable database with this explicit name prefix. */
@EnabledIfEnvironmentVariable(named = "HEALTHLINK_POSTGRES_TEST_URL", matches = ".+")
class PostgresMigrationTests {
    private DataSource dataSource;
    private String schema;

    @BeforeEach
    void createIsolatedSchema() throws SQLException {
        String url = System.getenv("HEALTHLINK_POSTGRES_TEST_URL");
        if (!url.matches("jdbc:postgresql://[^/]+/healthlink_maintenance_test_[a-z0-9]+")) {
            throw new IllegalArgumentException("PostgreSQL migration tests require a disposable healthlink_maintenance_test_* database");
        }
        dataSource = new DriverManagerDataSource(url, System.getenv("HEALTHLINK_POSTGRES_TEST_USER"),
                System.getenv("HEALTHLINK_POSTGRES_TEST_PASSWORD"));
        schema = "test_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
        }
    }

    @AfterEach
    void dropOnlyThisTestSchema() throws SQLException {
        if (schema != null && schema.matches("test_[a-f0-9]{32}")) {
            try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    @Test
    void freshMigrationsEnforceSingleOpenOrderAndAppendOnlyHistory() throws Exception {
        assertThat(flyway(false).migrate().migrationsExecuted).isEqualTo(2);
        try (var connection = connection(); var statement = connection.createStatement()) {
            statement.execute("INSERT INTO regions(id, code, name) VALUES (1, 'DEMO', 'Demo region')");
            statement.execute("INSERT INTO hospitals(id, code, name, city, address, region_id) VALUES (1, 'DEMO', 'Demo hospital', 'Fes', 'Demo', 1)");
            statement.execute("INSERT INTO departments(id, code, name, hospital_id) VALUES (1, 'RAD', 'Radiology', 1)");
            statement.execute("INSERT INTO imaging_types(id, code, name) VALUES (1, 'MRI', 'MRI')");
            statement.execute("INSERT INTO equipment(id, serial_number, model, status, status_updated_at, department_id, imaging_type_id) "
                    + "VALUES (1, 'DEMO', 'Demo', 'OUT_OF_SERVICE', CURRENT_TIMESTAMP, 1, 1)");
            statement.execute("INSERT INTO staff_members(id, full_name, email, keycloak_subject, role, hospital_id, department_id) "
                    + "VALUES (1, 'Demo Operator', 'operator@example.test', 'operator', 'RADIOLOGY_OPERATOR', 1, 1)");
            statement.execute("INSERT INTO staff_members(id, full_name, email, keycloak_subject, role, hospital_id) "
                    + "VALUES (2, 'Demo Technician', 'tech@example.test', 'tech', 'MAINTENANCE_TECHNICIAN', 1),"
                    + "(3, 'Demo Manager', 'manager@example.test', 'manager', 'HOSPITAL_MANAGER', 1)");
            statement.execute("INSERT INTO work_orders(id, equipment_id, reporter_id, status, description, reported_at, open_equipment_id) "
                    + "VALUES (1, 1, 1, 'OPEN', 'Fault', CURRENT_TIMESTAMP, 1)");
            assertThatThrownBy(() -> statement.execute("INSERT INTO work_orders(id, equipment_id, reporter_id, status, description, reported_at, open_equipment_id) "
                    + "VALUES (2, 1, 1, 'OPEN', 'Duplicate', CURRENT_TIMESTAMP, 1)"))
                    .isInstanceOf(SQLException.class).extracting(exception -> ((SQLException) exception).getSQLState()).isEqualTo("23505");
            assertThatThrownBy(() -> statement.execute("UPDATE work_orders SET status = 'VERIFIED', repairer_id = 2, verifier_id = 2, "
                    + "verified_at = CURRENT_TIMESTAMP, open_equipment_id = NULL WHERE id = 1"))
                    .isInstanceOf(SQLException.class).extracting(exception -> ((SQLException) exception).getSQLState()).isEqualTo("23514");
            statement.execute("UPDATE work_orders SET status = 'VERIFIED', repairer_id = 2, verifier_id = 3, "
                    + "verified_at = CURRENT_TIMESTAMP, open_equipment_id = NULL WHERE id = 1");
            statement.execute("INSERT INTO work_orders(id, equipment_id, reporter_id, status, description, reported_at, open_equipment_id) "
                    + "VALUES (2, 1, 1, 'OPEN', 'New fault', CURRENT_TIMESTAMP, 1)");

            statement.execute("INSERT INTO audit_records(id, occurred_at, action, equipment_id, hospital_id, actor_subject, actor_staff_id, "
                    + "actor_role, equipment_status, details) VALUES ('" + UUID.randomUUID()
                    + "', CURRENT_TIMESTAMP, 'FAULT_REPORTED', 1, 1, 'operator', 1, 'RADIOLOGY_OPERATOR', 'OUT_OF_SERVICE', 'Fault')");
            for (String mutation : new String[]{"UPDATE audit_records SET details = 'tampered'", "DELETE FROM audit_records", "TRUNCATE audit_records"}) {
                assertThatThrownBy(() -> statement.execute(mutation)).isInstanceOf(SQLException.class)
                        .hasMessageContaining("append-only");
            }
            try (var result = statement.executeQuery("SELECT COUNT(*) FROM audit_records")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isEqualTo(1);
            }
        }
    }

    @Test
    void baseliningPreservesLegacyDataAndAddsVersions() throws Exception {
        try (var connection = connection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/postgresql/V1__original_schema.sql"));
            try (var statement = connection.createStatement()) {
                statement.execute("INSERT INTO regions(id, code, name) VALUES (111, 'LEGACY', 'Preserved region')");
            }
        }
        assertThat(flyway(true).migrate().migrationsExecuted).isEqualTo(1);
        try (var connection = connection(); var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT name, version FROM regions WHERE id = 111")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getString(1)).isEqualTo("Preserved region");
            assertThat(result.getLong(2)).isZero();
        }
    }

    @Test
    void existingSchemasRequireExplicitBaselineOptIn() throws Exception {
        try (var connection = connection(); var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE sentinel (id BIGINT PRIMARY KEY)");
        }
        assertThatThrownBy(() -> flyway(false).migrate()).hasMessageContaining("non-empty");
    }

    @Test
    void migratedSchemaPassesProductionHibernateValidation() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("isolated-postgres-test", Map.of(
                "spring.datasource.url", System.getenv("HEALTHLINK_POSTGRES_TEST_URL") + "?currentSchema=" + schema,
                "spring.datasource.username", System.getenv("HEALTHLINK_POSTGRES_TEST_USER"),
                "spring.datasource.password", System.getenv("HEALTHLINK_POSTGRES_TEST_PASSWORD"),
                "spring.flyway.default-schema", schema,
                "spring.flyway.schemas", schema,
                "spring.flyway.baseline-on-migrate", "false",
                "spring.jpa.hibernate.ddl-auto", "validate",
                "server.port", "0",
                "logging.level.root", "WARN")));
        try (var context = new SpringApplicationBuilder(HealthLinkApplication.class).environment(environment).run()) {
            assertThat(context.getBean(jakarta.persistence.EntityManagerFactory.class).isOpen()).isTrue();
        }
    }

    private Flyway flyway(boolean baseline) {
        return Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                .locations("classpath:db/migration/postgresql").baselineOnMigrate(baseline).baselineVersion("1")
                .cleanDisabled(true).load();
    }

    private Connection connection() throws SQLException {
        Connection connection = dataSource.getConnection();
        connection.setSchema(schema);
        return connection;
    }
}
