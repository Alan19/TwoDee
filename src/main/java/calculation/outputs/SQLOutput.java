package calculation.outputs;

import calculation.models.DiceInfo;
import calculation.models.Info;
import calculation.models.RollInfo;
import io.vavr.control.Try;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rolling.Roller;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.Calendar;
import java.util.Optional;

public class SQLOutput implements IOutput, AutoCloseable {
    private static final Logger LOGGER = LogManager.getLogger(SQLOutput.class);

    private final Connection connection;
    private final String fileLocation;

    public SQLOutput(Connection connection, String fileLocation) {
        this.connection = connection;
        this.fileLocation = fileLocation;
    }

    @Override
    public void accept(Info object) {
        if (object instanceof RollInfo) {
            RollInfo rollInfo = ((RollInfo) object);
            Try.withResources(
                    connection::createStatement,
                    () -> connection.prepareStatement(
                            "INSERT INTO rolls(quote, rolledPool, playerName, total, flatBonus, " +
                                    "tier, exTier, enhancedAmount, enhancedTotal, enhancedTier, enhancedExTier, " +
                                    "doomAmount, doomPool) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    ),
                    () -> connection.prepareStatement(
                            "INSERT INTO dice(rollId, diceType, rolled, result) VALUES (?, ?, ?, ?)"
                    )
            ).of(((statement, rollPreparedStatement, dicePreparedStatement) -> {
                rollPreparedStatement.setString(1, rollInfo.getResultInfo().getQuote());
                rollPreparedStatement.setString(2, rollInfo.getResultInfo().getRolledPool());
                rollPreparedStatement.setString(3, rollInfo.getResultInfo().getPlayerName());
                rollPreparedStatement.setInt(4, rollInfo.getResultInfo().getTotal());
                rollPreparedStatement.setInt(5, rollInfo.getResultInfo().getFlatBonus());
                Pair<Optional<String>, Optional<String>> tiers = Roller.getTierHit(rollInfo.getResultInfo().getTotal());
                rollPreparedStatement.setString(6, tiers.getLeft().orElse("None"));
                if (tiers.getRight().isPresent()) {
                    rollPreparedStatement.setString(7, tiers.getRight().get());
                }
                else {
                    rollPreparedStatement.setNull(7, Types.VARCHAR);
                }
                if (rollInfo.getEnhancementAmount() > 0) {
                    rollPreparedStatement.setInt(8, rollInfo.getEnhancementAmount());
                    rollPreparedStatement.setInt(9, rollInfo.getEnhancementAmount() + rollInfo.getResultInfo().getTotal());
                    Pair<Optional<String>, Optional<String>> enhancedTiers = Roller.getTierHit(
                            rollInfo.getResultInfo().getTotal() + rollInfo.getEnhancementAmount()
                    );
                    rollPreparedStatement.setString(10, enhancedTiers.getLeft().orElse("None"));
                    if (enhancedTiers.getRight().isPresent()) {
                        rollPreparedStatement.setString(11, enhancedTiers.getRight().get());
                    }
                    else {
                        rollPreparedStatement.setNull(11, Types.VARCHAR);
                    }
                }
                else {
                    rollPreparedStatement.setNull(8, Types.INTEGER);
                    rollPreparedStatement.setNull(9, Types.INTEGER);
                    rollPreparedStatement.setNull(10, Types.VARCHAR);
                    rollPreparedStatement.setNull(11, Types.VARCHAR);
                }

                if (rollInfo.getDoomInfo() != null) {
                    rollPreparedStatement.setInt(12, rollInfo.getDoomInfo().getAmount());
                    rollPreparedStatement.setString(13, rollInfo.getDoomInfo().getPoolName());
                }
                else {
                    rollPreparedStatement.setNull(12, Types.INTEGER);
                    rollPreparedStatement.setNull(13, Types.VARCHAR);
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

    @Override
    public void close() throws Exception {
        connection.close();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static Try<SQLOutput> create(String channelName) {
        try {
            String fileName = "tmp/" + channelName + "-" + Calendar.getInstance().getTime().getTime() + ".db";
            Class.forName("org.sqlite.JDBC");
            new File("tmp").mkdirs();
            Connection c = DriverManager.getConnection("jdbc:sqlite:" + fileName);

            Try.withResources(c::createStatement)
                    .of(statement -> {
                        statement.execute(
                                "CREATE TABLE IF NOT EXISTS rolls(" +
                                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
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
                                        "doomPool VARCHAR(256) " +
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
                    });
            return Try.success(new SQLOutput(c, fileName));
        } catch (Throwable throwable) {
            return Try.failure(throwable);
        }
    }

    @Nullable
    @Override
    public Path getFilePath() {
        return Paths.get(fileLocation);
    }
}
