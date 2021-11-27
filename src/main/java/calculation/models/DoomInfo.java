package calculation.models;

import io.vavr.control.Try;
import org.javacord.api.entity.message.embed.Embed;
import util.RegexExtractor;

/**
 * A Holding class for the Results from a Doom Embed. Split out for parsing simplicity
 */
public class DoomInfo {
    private final String poolName;
    private final int amount;

    public DoomInfo(String poolName, int amount) {
        this.poolName = poolName;
        this.amount = amount;
    }

    public int getAmount() {
        return amount;
    }

    public String getPoolName() {
        return poolName;
    }

    @Override
    public String toString() {
        return "DoomInfo{" +
                "poolName='" + poolName + '\'' +
                ", amount=" + amount +
                '}';
    }

    public static boolean isValid(Embed embed) {
        return embed.getTitle().filter("An opportunity!"::equalsIgnoreCase).isPresent();
    }

    public static Try<DoomInfo> from(Embed embed) {
        if (isValid(embed)) {
            return embed.getFields()
                    .stream()
                    .filter(embedField -> !"plot points".equalsIgnoreCase(embedField.getName()))
                    .findFirst()
                    .map(poolField -> RegexExtractor.getDifference(poolField.getValue())
                            .map(value -> new DoomInfo(poolField.getName(), value))
                    )
                    .orElseGet(() -> Try.failure(new IllegalStateException("Failed to find Valid Pool Field")));


        }
        else {
            return Try.failure(new IllegalArgumentException("Embed is not a Valid Doom Embed"));
        }
    }
}
