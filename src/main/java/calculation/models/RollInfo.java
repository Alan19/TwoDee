package calculation.models;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.vavr.control.Try;
import org.apache.commons.lang3.tuple.Pair;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.Embed;
import rolling.Roller;
import util.RegexExtractor;

import java.util.Optional;

public class RollInfo extends Info {
    private final ResultInfo resultInfo;
    private final DoomInfo doomInfo;
    private final int enhancementAmount;

    public RollInfo(ResultInfo resultInfo, DoomInfo doomInfo, int enhancementAmount) {
        this.resultInfo = resultInfo;
        this.doomInfo = doomInfo;
        this.enhancementAmount = enhancementAmount;
    }

    public ResultInfo getResultInfo() {
        return resultInfo;
    }

    public DoomInfo getDoomInfo() {
        return doomInfo;
    }

    public int getEnhancementAmount() {
        return enhancementAmount;
    }

    @Override
    public String toString() {
        return "RollInfo{" +
                "resultInfo=" + resultInfo +
                ", doomInfo=" + doomInfo +
                ", enhancedAmount=" + enhancementAmount +
                '}';
    }

    @Override
    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("quote", this.resultInfo.getQuote());
        jsonObject.addProperty("rolledPool", this.resultInfo.getRolledPool());
        jsonObject.addProperty("playerName", this.resultInfo.getPlayerName());
        jsonObject.addProperty("total", this.resultInfo.getTotal());
        jsonObject.addProperty("flatBonus", this.resultInfo.getFlatBonus());
        Pair<Optional<String>, Optional<String>> tiers = Roller.getTierHit(this.resultInfo.getTotal());
        jsonObject.addProperty("tier", tiers.getLeft().orElse("None"));
        tiers.getRight()
                .ifPresent(exTier -> jsonObject.addProperty("exTier", exTier));
        if (enhancementAmount > 0) {
            jsonObject.addProperty("enhancementAmount", this.enhancementAmount);
            jsonObject.addProperty("enhancementTotal", this.enhancementAmount + this.resultInfo.getTotal());
            Pair<Optional<String>, Optional<String>> enhancedTiers = Roller.getTierHit(this.resultInfo.getTotal() + this.enhancementAmount);
            jsonObject.addProperty("enhancedTier", enhancedTiers.getLeft().orElse("None"));
            enhancedTiers.getRight()
                    .ifPresent(exTier -> jsonObject.addProperty("enhancedExTier", exTier));
        }
        if (doomInfo != null) {
            jsonObject.addProperty("doomAmount", this.doomInfo.amount());
            jsonObject.addProperty("doomPool", this.doomInfo.poolName());
        }
        JsonArray diceArray = new JsonArray();
        for (DiceInfo diceInfo : this.resultInfo.getDice()) {
            JsonObject diceObject = new JsonObject();
            diceObject.addProperty("type", diceInfo.getDiceType().getAbbreviation());
            diceObject.addProperty("rolled", diceInfo.getRolled());
            diceObject.addProperty("result", diceInfo.getResult());
            diceArray.add(diceObject);
        }
        jsonObject.add("dice", diceArray);

        return jsonObject;
    }

    public static boolean isValid(Message message) {
        return message.getEmbeds()
                .stream()
                .anyMatch(ResultInfo::isValid);
    }

    public static Try<RollInfo> fromMessage(Message message) {
        Try<DoomInfo> doomInfoTry = Try.success(null);
        Try<ResultInfo> resultInfoTry = Try.failure(new IllegalStateException("No Result Embed found"));
        Try<Integer> enhancedTry = Try.success(0);

        if (!message.getEmbeds().isEmpty()) {
            for (Embed embed : message.getEmbeds()) {
                if (DoomInfo.isValid(embed)) {
                    doomInfoTry = DoomInfo.from(embed);
                }
                else if (ResultInfo.isValid(embed)) {
                    resultInfoTry = ResultInfo.from(embed);
                }
                else if (embed.getTitle().filter("Enhancing a roll!"::equalsIgnoreCase).isPresent()) {
                    enhancedTry = embed.getFields()
                            .stream()
                            .filter(embedField -> embedField.getName().equalsIgnoreCase("Enhanced Total"))
                            .findFirst()
                            .map(embedField -> RegexExtractor.getDifference(embedField.getValue()))
                            .orElseGet(() -> Try.failure(new IllegalArgumentException("Failed to find Enhanced Total field")));
                }
            }
        }
        else {
            return Try.failure(new IllegalArgumentException("Message has no Embeds"));
        }

        if (resultInfoTry.isFailure()) {
            return resultInfoTry.map(value -> null);
        }
        else if (doomInfoTry.isFailure()) {
            return doomInfoTry.map(value -> null);
        }
        else if (enhancedTry.isFailure()) {
            return enhancedTry.map(value -> null);
        }
        else {
            return Try.success(new RollInfo(resultInfoTry.get(), doomInfoTry.get(), enhancedTry.get()));
        }
    }
}
