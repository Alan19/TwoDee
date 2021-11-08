package calculation.models;

import io.vavr.control.Try;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.Embed;
import util.RegexExtractor;

public class RollInfo {
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
