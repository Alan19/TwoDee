package calculation.models;

import io.vavr.control.Try;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.entity.message.embed.EmbedAuthor;
import org.javacord.api.entity.message.embed.EmbedField;
import rolling.Dice;
import rolling.Roller;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ResultInfo {
    private static final Pattern EXTRACT_POOL = Pattern.compile("\\b\\d*[eEkKcCpP]?[d|D]\\d+");

    private final String quote;
    private final String rolledPool;
    private final String playerName;
    private final int total;
    private final int flatBonus;
    private final List<DiceInfo> dice;

    public ResultInfo(String quote, String rolledPool, String playerName, int total, int flatBonus, List<DiceInfo> dice) {
        this.quote = quote;
        this.rolledPool = rolledPool;
        this.playerName = playerName;
        this.total = total;
        this.flatBonus = flatBonus;
        this.dice = dice;
    }

    public String getQuote() {
        return quote;
    }

    public int getFlatBonus() {
        return flatBonus;
    }

    public int getTotal() {
        return total;
    }

    public List<DiceInfo> getDice() {
        return dice;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getRolledPool() {
        return rolledPool;
    }

    @Override
    public String toString() {
        return "ResultInfo{" +
                "quote='" + quote + '\'' +
                ", rolledPool='" + rolledPool + '\'' +
                ", playerName='" + playerName + '\'' +
                ", total=" + total +
                ", flatBonus=" + flatBonus +
                ", dice=" + dice +
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
                    .filter(value -> !value.contains("none"))
                    .map(value -> Arrays.stream(value.replace("*", "").split(","))
                            .map(String::trim)
                            .mapToInt(Integer::parseInt)
                            .sum()
                    )
                    .orElse(0);

            return embed.getDescription()
                    .map(description -> extractPool(description)
                            .flatMap(pool -> extractDice(pool, fieldValues)
                                    .map(diceInfos -> new ResultInfo(
                                            quote,
                                            pool,
                                            playerName,
                                            total,
                                            flatBonus,
                                            diceInfos
                                    ))
                            )
                    )
                    .orElseGet(() -> Try.failure(new IllegalArgumentException("Embed does not have a description")));
        }
        else {
            return Try.failure(new IllegalArgumentException("Embed is not a Valid Result Embed"));
        }
    }

    public static Try<String> extractPool(String rolledPool) {
        Matcher matcher = EXTRACT_POOL.matcher(" " + rolledPool);
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

    public static Try<List<DiceInfo>> extractDice(String pool, Map<String, String> fields) {
        return Roller.parse(pool, name -> Try.failure(new IllegalArgumentException("no skills allow")))
                .flatMap(pair -> {
                    List<DiceInfo> diceInfo = new ArrayList<>();
                    List<Dice> diceList = pair.getLeft();
                    Iterator<Integer> regularChaos = getIteratorFromField(fields, "regular and chaos dice", "regular dice");
                    Iterator<Integer> plotEnhanced = getIteratorFromField(fields, "plot dice");
                    Iterator<Integer> kept = getIteratorFromField(fields, "kept dice");
                    for (Dice dice : diceList) {
                        switch (dice.getType()) {
                            case REGULAR:
                            case CHAOS_DIE:
                                if (regularChaos.hasNext()) {
                                    diceInfo.add(new DiceInfo(
                                            dice.getType(),
                                            dice.getValue(),
                                            regularChaos.next()
                                    ));
                                }
                                else {
                                    return Try.failure(new IllegalStateException("Tried to get value for " + dice.getType()));
                                }
                                break;
                            case PLOT_DIE:
                            case ENHANCED_DIE:
                                if (plotEnhanced.hasNext()) {
                                    diceInfo.add(new DiceInfo(
                                            dice.getType(),
                                            dice.getValue(),
                                            plotEnhanced.next()
                                    ));
                                }
                                else {
                                    return Try.failure(new IllegalStateException("Tried to get value for " + dice.getType()));
                                }
                                break;
                            case KEPT_DIE:
                                if (kept.hasNext()) {
                                    diceInfo.add(new DiceInfo(
                                            dice.getType(),
                                            dice.getValue(),
                                            kept.next()
                                    ));
                                }
                                else {
                                    return Try.failure(new IllegalStateException("Tried to get value for " + dice.getType()));
                                }
                                break;
                        }
                    }
                    return Try.success(diceInfo);
                });
    }

    public static Iterator<Integer> getIteratorFromField(Map<String, String> fields, String... fieldNames) {
        String resultString = null;
        Iterator<String> fieldNameIterator = Arrays.asList(fieldNames).listIterator();
        while (resultString == null && fieldNameIterator.hasNext()) {
            resultString = fields.get(fieldNameIterator.next());
        }

        if (resultString != null && !resultString.contains("none")) {
            return Arrays.stream(resultString.replace("*", "").split(","))
                    .map(String::trim)
                    .mapToInt(Integer::parseInt)
                    .iterator();
        }

        return Collections.emptyIterator();
    }
}
