package calculation;

import calculation.models.RollInfo;
import io.vavr.control.Try;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;

import java.util.Objects;

public class CalculationLogic {
    public static void beginCalculations(DiscordApi api, TextChannel channel, Long startingMessage, Long endingMessage) {
        Try<CalculationStore> calculationStore = CalculationStore.create();

        channel.getMessagesBetweenAsStream(startingMessage, endingMessage)
                .map(message -> {
                    if (RollInfo.isValid(message)) {
                        return RollInfo.fromMessage(message);
                    }
                    else {
                        return Try.success(null);
                    }
                })
                .filter(Objects::nonNull)
                .filter(tried -> tried.isFailure() || tried.get() != null)
                .forEach(System.out::println);
    }
}
