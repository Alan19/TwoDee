package calculation.models;

import io.vavr.control.Try;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.entity.message.embed.EmbedAuthor;
import org.javacord.api.entity.message.embed.EmbedField;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ResultInfo {
    private static final Pattern EXTRACT_POOL = Pattern.compile("\\d*[eEkKcCpP]?[d|D]\\d+");

    private final String quote;
    private final String rolledPool;
    private final String playerName;
    private final int total;
    private final int flatBonus;

    public ResultInfo(String quote, String rolledPool, String playerName, int total, int flatBonus) {
        this.quote = quote;
        this.rolledPool = rolledPool;
        this.playerName = playerName;
        this.total = total;
        this.flatBonus = flatBonus;
    }

    @Override
    public String toString() {
        return "ResultInfo{" +
                "quote='" + quote + '\'' +
                ", rolledPool='" + rolledPool + '\'' +
                ", playerName='" + playerName + '\'' +
                ", total=" + total +
                ", flatBonus=" + flatBonus +
                '}';
    }

    public static boolean isValid(Embed embed) {
        return embed.getAuthor().isPresent() && embed.getDescription().isPresent() && !embed.getFields().isEmpty();
    }

    public static Try<ResultInfo> from(Embed embed) {
        if (isValid(embed)) {
            String quote = embed.getTitle()
                    .orElse("");
            String playerName = embed.getAuthor()
                    .map(EmbedAuthor::getName)
                    .orElse("");

            Map<String, String> fieldValues = embed.getFields()
                    .stream()
                    .collect(Collectors.toMap(
                            embedField -> embedField.getName()
                                    .toLowerCase(Locale.ROOT)
                                    .trim(),
                            EmbedField::getValue
                    ));

            int total = Optional.ofNullable(fieldValues.get("total"))
                    .map(Integer::parseInt)
                    .orElse(0);

            int flatBonus = Optional.ofNullable(fieldValues.get("flat bonuses"))
                    .filter("none"::equalsIgnoreCase)
                    .map(value -> Arrays.stream(value.split(","))
                            .map(String::trim)
                            .mapToInt(Integer::parseInt)
                            .sum()
                    )
                    .orElse(0);


            return embed.getDescription()
                    .map(description -> extractPool(description)
                            .map(pool -> new ResultInfo(
                                    quote,
                                    pool,
                                    playerName,
                                    total,
                                    flatBonus))
                    )
                    .orElseGet(() -> Try.failure(new IllegalArgumentException("Emded does not have a description")));
        }
        else {
            return Try.failure(new IllegalArgumentException("Embed is not a Valid Result Embed"));
        }
    }

    public static Try<String> extractPool(String rolledPool) {
        Matcher matcher = EXTRACT_POOL.matcher(rolledPool);
        StringBuilder dice = new StringBuilder();
        while (matcher.find()) {
            for (int i = 0; i <= matcher.groupCount(); i++) {
                dice.append(matcher.group());
                dice.append(" ");
            }
        }
        String pool = dice.toString().trim();
        if (pool.isEmpty()) {
            return Try.failure(new IllegalArgumentException("Failed to extract pool value"));
        }
        else {
            return Try.success(pool);
        }
    }
}
