package logic;

import calculation.CalculationCollector;
import calculation.models.CalculationStats;
import calculation.models.Info;
import calculation.models.RollInfo;
import calculation.outputs.OutputType;
import exceptions.InvalidUserInputException;
import exceptions.UserException;
import io.vavr.Tuple;
import io.vavr.control.Try;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.ChannelType;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.interaction.*;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import pw.mihou.velen.interfaces.*;
import pw.mihou.velen.interfaces.routed.VelenRoutedOptions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

/**
 * Command Class for Calculation. Handles IO, as well as streaming all messages into the Collector
 */
public class CalculationLogic implements VelenEvent, VelenSlashEvent {
    private final static Logger LOGGER = LogManager.getLogger(CalculationLogic.class);

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void onEvent(MessageCreateEvent event, Message message, User user, String[] args, VelenRoutedOptions options) {
        Message startingMessage = message.reply("Staring Calculations").join();
        Try.of(() -> Long.parseLong(args[0]))
                .flatMap(start -> CalculationLogic.beginCalculations(
                        args.length > 1 ? OutputType.getByName(args[1]).orElse(OutputType.SQLITE) : OutputType.SQLITE,
                        event.getChannel(),
                        start,
                        args.length > 2 ? Long.parseLong(args[2]) : message.getId(),
                        update -> startingMessage.edit("Handled " + update + " Rolls")
                                .join()
                ))
                .fold(
                        throwable -> {
                            if (throwable instanceof UserException) {
                                return message.reply("Invalid Input: " + throwable.getMessage());
                            }
                            else {
                                LOGGER.warn("Error while generating Stats", throwable);
                                return message.reply("Exception while generating Stats. Check Logs");
                            }
                        },
                        stats -> event.getChannel()
                                .sendMessage(
                                        String.format("Calculations Complete: %s Successful, %s Skipped, %s Errored, Top Error was '%s'",
                                                stats.getSuccess(),
                                                stats.getSkipped(),
                                                stats.getError(),
                                                stats.getTopError()
                                        ),
                                        stats.getFile()
                                )
                                .thenApply(finishedMessage -> {
                                    if (stats.getFile() != null) {
                                        stats.getFile().delete();
                                    }
                                    return finishedMessage;
                                })
                );


    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void onEvent(SlashCommandCreateEvent originalEvent, SlashCommandInteraction event, User user, VelenArguments args,
                        List<SlashCommandInteractionOption> options, InteractionImmediateResponseBuilder firstResponder) {
        firstResponder.setContent("Starting Calculations")
                .respond()
                .thenCompose(updater ->
                        Try.of(() -> Tuple.of(
                                        event.getOptionStringValueByName("output")
                                                .flatMap(OutputType::getByName)
                                                .orElse(OutputType.SQLITE),
                                        event.getOptionChannelValueByName("channel")
                                                .flatMap(Channel::asTextChannel)
                                                .orElseGet(() -> event.getChannel()
                                                        .orElseThrow(() -> new InvalidUserInputException("No Valid Channel Found"))
                                                ),
                                        event.getOptionStringValueByName("start")
                                                .map(Long::parseLong)
                                                .orElseThrow(() -> new InvalidUserInputException("No Start Message Provided")),
                                        event.getOptionStringValueByName("end")
                                                .map(Long::parseLong)
                                                .orElseThrow(() -> new InvalidUserInputException("No End Message Provider"))
                                ))
                                .flatMap(tuple -> CalculationLogic.beginCalculations(
                                        tuple._1,
                                        tuple._2,
                                        tuple._3,
                                        tuple._4,
                                        rolls -> updater.setContent("Handled " + rolls + " Rolls")
                                                .update()
                                                .join()
                                ))
                                .fold(throwable -> {
                                            if (throwable instanceof UserException) {
                                                return updater.setContent("Invalid Input: " + throwable.getMessage())
                                                        .update();
                                            }
                                            else {

                                                LOGGER.warn("Failed to generate Stats", throwable);
                                                return updater.setContent("Exception while generating Stats. Check Logs")
                                                        .update();
                                            }
                                        },
                                        stats -> updater.setContent(String.format(
                                                        "Calculations Complete: %s Successful, %s Skipped, %s Errored, Top Error was '%s'",
                                                        stats.getSuccess(),
                                                        stats.getSkipped(),
                                                        stats.getError(),
                                                        stats.getTopError()
                                                ))
                                                .addAttachment(stats.getFile())
                                                .update()
                                                .thenApply(future -> {
                                                    if (stats.getFile() != null) {
                                                        stats.getFile().delete();
                                                    }
                                                    return future;
                                                })
                                )

                );

    }

    public static void setup(Velen velen) {
        CalculationLogic calculationCommand = new CalculationLogic();

        VelenCommand.ofHybrid("calculation", "Runs Calculations of Roll Results", velen, calculationCommand, calculationCommand)
                .addOption(SlashCommandOption.create(
                        SlashCommandOptionType.STRING,
                        "start",
                        "Message Id for the starting message",
                        true
                ))
                .addOption(SlashCommandOption.create(
                        SlashCommandOptionType.STRING,
                        "end",
                        "Message Id for the ending message",
                        true
                ))
                .addOption(SlashCommandOption.createChannelOption(
                        "channel",
                        "the channel to run calculations for",
                        false,
                        Collections.singleton(ChannelType.SERVER_TEXT_CHANNEL)
                ))
                .addOption(SlashCommandOption.createWithChoices(
                        SlashCommandOptionType.STRING,
                        "output",
                        "The Output Type for the File",
                        false,
                        Arrays.stream(OutputType.values())
                                .map(type -> new SlashCommandOptionChoiceBuilder()
                                        .setName(type.name().toLowerCase(Locale.ROOT))
                                        .setValue(type.name().toLowerCase(Locale.ROOT))
                                        .build()
                                )
                                .collect(Collectors.toList())
                ))
                .addShortcut("calc")
                .attach();
    }

    public static Try<CalculationStats> beginCalculations(OutputType type, TextChannel channel, Long startingMessage,
                                                          Long endingMessage, IntConsumer updateHandler) {
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
                                .collect(new CalculationCollector(consumer, updateHandler))
                );
    }
}
