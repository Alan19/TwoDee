package calculation;

import io.vavr.control.Try;

import java.sql.Connection;
import java.sql.DriverManager;

public class CalculationStore implements AutoCloseable {
    private final Connection connection;

    public CalculationStore(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void close() throws Exception {
        connection.close();
    }

    public static Try<CalculationStore> create() {
        try {
            Class.forName("org.sqlite.JDBC");
            Connection c = DriverManager.getConnection("jdbc:sqlite:resources/calculations.db");
            return Try.success(new CalculationStore(c));
        } catch (Throwable throwable) {
            return Try.failure(throwable);
        }
    }
}
