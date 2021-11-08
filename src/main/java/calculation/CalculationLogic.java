package calculation;

import calculation.models.RollInfo;
import io.vavr.control.Try;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;

import java.util.Objects;
import java.util.function.Consumer;

public class CalculationLogic {
    private static final Logger LOGGER = LogManager.getLogger(CalculationLogic.class);

    public static void beginCalculations(DiscordApi api, TextChannel channel, Long startingMessage, Long endingMessage) {
        Try<Consumer<Object>> consumers = CalculationStore.create()
                .flatMap(calculationStore ->
                        calculationStore.startRunning(channel.getId(), startingMessage, endingMessage)
                );

        consumers.fold(failure -> {
            channel.sendMessage("Failed to Setup Calculations: " + failure.getMessage());
            failure.printStackTrace();
            return null;
        }, consumer -> {
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
                    .forEach(tried -> tried.fold(
                            failure -> {
                                LOGGER.warn("Failed to handle Message", failure.getCause());
                                return null;
                            },
                            success -> {
                                if (success != null) {
                                    consumer.accept(success);
                                }
                                return null;
                            }
                    ));

            return null;
        });


    }
}
