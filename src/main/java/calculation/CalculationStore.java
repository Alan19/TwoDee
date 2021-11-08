package calculation;

import calculation.models.DiceInfo;
import calculation.models.RollInfo;
import io.vavr.control.Try;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rolling.Roller;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class CalculationStore implements AutoCloseable {
    private static final Logger LOGGER = LogManager.getLogger(CalculationStore.class);

    private final Connection connection;
    private final UUID calculationKey;

    private boolean started;
    private int calculationId;

    public CalculationStore(Connection connection) {
        this.connection = connection;
        this.calculationKey = UUID.randomUUID();
    }

    public Try<Consumer<Object>> startRunning(long channelId, long startMessage, long endMessage) {
        return Try.withResources(connection::createStatement)
                .of(statement -> {
                    statement.execute(
                            "CREATE TABLE IF NOT EXISTS calculations(" +
                                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                    "key VARCHAR(256) NOT NULL, " +
                                    "channelId INTEGER NOT NULL, " +
                                    "startMessage INTEGER NOT NULL, " +
                                    "endMessage INTEGER NOT NULL" +
                                    ")"
                    );

                    statement.execute(
                            "CREATE TABLE IF NOT EXISTS rolls(" +
                                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                    "calculationsId INTEGER NOT NULL, " +
                                    "quote VARCHAR(256) NOT NULL, " +
                                    "rolledPool VARCHAR(256) NOT NULL, " +
                                    "playerName VARCHAR(256) NOT NULL, " +
                                    "total INTEGER NOT NULL, " +
                                    "flatBonus INTEGER NOT NULL, " +
                                    "tier VARCHAR(256) NOT NULL, " +
                                    "exTier VARCHAR(256), " +
                                    "enhancedAmount INTEGER, " +
                                    "enhancedTotal INTEGER, " +
                                    "enhancedTier VARCHAR(256), " +
                                    "enhancedExTier VARCHAR(256), " +
                                    "doomAmount INTEGER, " +
                                    "doomPool VARCHAR(256), " +
                                    "FOREIGN KEY (calculationsId) REFERENCES calculations(id)" +
                                    ")"
                    );

                    statement.execute(
                            "CREATE TABLE IF NOT EXISTS dice(" +
                                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                    "rollId INTEGER NOT NULL, " +
                                    "diceType VARCHAR(16) NOT NULL, " +
                                    "rolled INTEGER NOT NULL, " +
                                    "result INTEGER NOT NULL, " +
                                    "FOREIGN KEY(rollId) REFERENCES roll(id)" +
                                    ")"
                    );

                    return null;
                })
                .flatMap(nothing -> Try.withResources(connection::createStatement, () -> connection.prepareStatement(
                        "INSERT INTO calculations(channelId, key, startMessage, endMessage) VALUES(?,?,?,?)"
                )).of((statement, preparedStatement) -> {
                    preparedStatement.setLong(1, channelId);
                    preparedStatement.setString(2, calculationKey.toString());
                    preparedStatement.setLong(3, startMessage);
                    preparedStatement.setLong(4, endMessage);

                    preparedStatement.execute();

                    ResultSet set = statement.executeQuery("SELECT last_insert_rowid()");
                    if (set.next()) {
                        this.calculationId = set.getInt(1);
                        this.started = true;

                        return this::insert;
                    }
                    else {
                        throw new IllegalStateException("Failed to get calculationId");
                    }
                }));
    }

    private void insert(Object object) {
        if (started) {
            if (object instanceof RollInfo) {
                RollInfo rollInfo = ((RollInfo) object);
                Try.withResources(
                        connection::createStatement,
                        () -> connection.prepareStatement(
                                "INSERT INTO rolls(calculationsId, quote, rolledPool, playerName, total, flatBonus, " +
                                        "tier, exTier, enhancedAmount, enhancedTotal, enhancedTier, enhancedExTier, " +
                                        "doomAmount, doomPool) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                        ),
                        () -> connection.prepareStatement(
                                "INSERT INTO dice(rollId, diceType, rolled, result) VALUES (?, ?, ?, ?)"
                        )
                ).of(((statement, rollPreparedStatement, dicePreparedStatement) -> {
                    rollPreparedStatement.setLong(1, calculationId);
                    rollPreparedStatement.setString(2, rollInfo.getResultInfo().getQuote());
                    rollPreparedStatement.setString(3, rollInfo.getResultInfo().getRolledPool());
                    rollPreparedStatement.setString(4, rollInfo.getResultInfo().getPlayerName());
                    rollPreparedStatement.setInt(5, rollInfo.getResultInfo().getTotal());
                    rollPreparedStatement.setInt(6, rollInfo.getResultInfo().getFlatBonus());
                    Pair<Optional<String>, Optional<String>> tiers = Roller.getTierHit(rollInfo.getResultInfo().getTotal());
                    rollPreparedStatement.setString(7, tiers.getLeft().orElse("None"));
                    if (tiers.getRight().isPresent()) {
                        rollPreparedStatement.setString(8, tiers.getRight().get());
                    }
                    else {
                        rollPreparedStatement.setNull(8, Types.VARCHAR);
                    }
                    if (rollInfo.getEnhancementAmount() > 0) {
                        rollPreparedStatement.setInt(9, rollInfo.getEnhancementAmount());
                        rollPreparedStatement.setInt(10, rollInfo.getEnhancementAmount() + rollInfo.getResultInfo().getTotal());
                        Pair<Optional<String>, Optional<String>> enhancedTiers = Roller.getTierHit(
                                rollInfo.getResultInfo().getTotal() + rollInfo.getEnhancementAmount()
                        );
                        rollPreparedStatement.setString(11, enhancedTiers.getLeft().orElse("None"));
                        if (enhancedTiers.getRight().isPresent()) {
                            rollPreparedStatement.setString(12, enhancedTiers.getRight().get());
                        }
                        else {
                            rollPreparedStatement.setNull(12, Types.VARCHAR);
                        }
                    }
                    else {
                        rollPreparedStatement.setNull(9, Types.INTEGER);
                        rollPreparedStatement.setNull(10, Types.INTEGER);
                        rollPreparedStatement.setNull(11, Types.VARCHAR);
                        rollPreparedStatement.setNull(12, Types.VARCHAR);
                    }

                    if (rollInfo.getDoomInfo() != null) {
                        rollPreparedStatement.setInt(13, rollInfo.getDoomInfo().getAmount());
                        rollPreparedStatement.setString(14, rollInfo.getDoomInfo().getPoolName());
                    }
                    else {
                        rollPreparedStatement.setNull(13, Types.INTEGER);
                        rollPreparedStatement.setNull(14, Types.VARCHAR);
                    }

                    rollPreparedStatement.execute();

                    ResultSet set = statement.executeQuery("SELECT last_insert_rowid()");
                    if (set.next()) {
                        int rollId = set.getInt(1);
                        for (DiceInfo diceInfo : rollInfo.getResultInfo().getDice()) {
                            dicePreparedStatement.setInt(1, rollId);
                            dicePreparedStatement.setString(2, diceInfo.getDiceType().toString());
                            dicePreparedStatement.setInt(3, diceInfo.getRolled());
                            dicePreparedStatement.setInt(4, diceInfo.getResult());
                            dicePreparedStatement.addBatch();
                        }
                        dicePreparedStatement.executeBatch();
                    }
                    else {
                        throw new IllegalStateException("Failed to get rollId");
                    }

                    return null;
                })).fold(
                        throwable -> {
                            LOGGER.warn("Failed to Insert Rolls", throwable);
                            return null;
                        },
                        nothing -> null
                );
            }
        }
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
