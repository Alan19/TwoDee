package calculation;

import calculation.models.CalculationStats;
import calculation.models.Info;
import calculation.models.RollInfo;
import calculation.outputs.OutputType;
import io.vavr.control.Try;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;

public class CalculationLogic {
    public static Try<CalculationStats> beginCalculations(OutputType type, DiscordApi api, TextChannel channel, Long startingMessage, Long endingMessage) {
        return type.setup(channel.getIdAsString())
                .flatMap(consumer ->
                        channel.getMessagesBetweenAsStream(startingMessage, endingMessage)
                                .map(message -> {
                                    if (RollInfo.isValid(message)) {
                                        return RollInfo.fromMessage(message);
                                    }
                                    else {
                                        return Try.<Info>success(null);
                                    }
                                })
                                .collect(new CalculationCollector(consumer))
                );
    }
}
