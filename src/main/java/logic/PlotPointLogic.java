package logic;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.interaction.*;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import org.javacord.api.util.DiscordRegexPattern;
import pw.mihou.velen.interfaces.*;
import sheets.PlotPointUtils;
import sheets.SheetsHandler;
import util.RandomColor;
import util.UtilFunctions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;

/**
 * This class adds plot points, subtracts plot points, and sets plot points for players. This class will also keep
 * track of doom points
 */
public class PlotPointLogic implements VelenSlashEvent, VelenEvent {

    public static void registerPlotPointCommand(Velen velen) {
        PlotPointLogic plotPointLogic = new PlotPointLogic();
        final List<SlashCommandOption> slashCommandOptions = new ArrayList<>();
        slashCommandOptions.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "add", "Adds to the specified plot point pool(s)", getMentionableOption(), getPlotPointCountOption()));
        slashCommandOptions.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "sub", "Subtracts from the specified plot point pool(s)", getMentionableOption(), getPlotPointCountOption()));
        slashCommandOptions.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "set", "Sets the specified plot point pools to the specified amount", getCountOption().setRequired(true), getMentionableOption()));
        slashCommandOptions.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "query", "Queries the value of all plot point pools", getMentionableOption().setDescription("which player to query the plot point pool of")));

        VelenCommand.ofHybrid("plotpoints", "Adjust plot points", velen, plotPointLogic, plotPointLogic).addShortcuts("p", "pp", "plotpoints").addOption(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND_GROUP, "mode", "how to modify the plot point pools", slashCommandOptions)).attach();
    }

    private static SlashCommandOptionBuilder getMentionableOption() {
        return new SlashCommandOptionBuilder()
                .setType(SlashCommandOptionType.USER)
                .setName("name")
                .setDescription("the user to target with the command")
                .setRequired(false);
    }

    private static SlashCommandOptionBuilder getCountOption() {
        return new SlashCommandOptionBuilder()
                .setName("count")
                .setDescription("the amount to modify the doom pool by")
                .setType(SlashCommandOptionType.INTEGER)
                .setRequired(false);
    }

    private static SlashCommandOptionBuilder getPlotPointCountOption() {
        return new SlashCommandOptionBuilder()
                .setName("count")
                .setDescription("the amount to modify the plot point pool by")
                .setType(SlashCommandOptionType.INTEGER)
                .setRequired(false);
    }

    @Override
    public void onEvent(MessageCreateEvent event, Message message, User user, String[] args) {
        if (args.length > 0) {
            String mode = args[0];
            User target = user;
            int count = 1;
            if (args.length > 1) {
                for (int i = 1; i < args.length; i++) {
                    String arg = args[i];
                    final Matcher matcher = DiscordRegexPattern.USER_MENTION.matcher(arg);
                    if (matcher.find()) {
                        target = event.getApi().getUserById(matcher.group("id")).join();
                    }
                    else if (UtilFunctions.tryParseInt(arg).isPresent()) {
                        count = UtilFunctions.tryParseInt(arg).orElse(1);
                    }
                }
            }
            executeCommand(user, mode, target, count, event.getChannel()).thenAccept(embedBuilder -> new MessageBuilder().addEmbed(embedBuilder).send(event.getChannel()));
        }
        else {
            executeCommand(user, "query", user, 0, event.getChannel()).thenAccept(embedBuilder -> new MessageBuilder().addEmbed(embedBuilder).send(event.getChannel()));
        }
    }

    @Override
    public void onEvent(SlashCommandInteraction event, User user, VelenArguments args, List<SlashCommandInteractionOption> options, InteractionImmediateResponseBuilder firstResponder) {
        final Optional<SlashCommandInteractionOption> mode = event.getOptionByName("mode").flatMap(option -> option.getOptionByIndex(1));
        final Optional<User> mentionedUser = mode.flatMap(slashCommandInteractionOption -> slashCommandInteractionOption.getOptionUserValueByName("name"));
        final Optional<Integer> count = mode.flatMap(slashCommandInteractionOption -> slashCommandInteractionOption.getOptionLongValueByName("count").map(Math::toIntExact));
        if (event.getChannel().isPresent()) {
            final CompletableFuture<EmbedBuilder> query = executeCommand(user, mode.map(SlashCommandInteractionOption::getName).orElse("query"), mentionedUser.orElse(user), count.orElse(1), event.getChannel().get());
            event.respondLater().thenAcceptBoth(query, (updater, embed) -> updater.addEmbed(embed).update());
        }
        else {
            firstResponder.setContent("Unable to find a channel!").respond();
        }
    }

    private CompletableFuture<EmbedBuilder> executeCommand(User sender, String mode, User target, Integer count, TextChannel channel) {
        CompletableFuture<EmbedBuilder> embed;
        switch (mode) {
            case "add":
                embed = addPointsAndGetEmbed(target, count, channel);
                break;
            case "sub":
                embed = addPointsAndGetEmbed(target, count * -1, channel);
                break;
            case "set":
                embed = setPointsAndGetEmbed(target, count, channel);
                break;
            default:
                embed = getPlotPointEmbed(target, channel);
                break;
        }
        return embed.thenApply(builder -> builder.setTitle("Plot Points!")
                .setColor(RandomColor.getRandomColor())
                .setFooter("Requested by " + UtilFunctions.getUsernameInChannel(sender, channel), sender.getAvatar()));
    }

    private CompletableFuture<EmbedBuilder> getPlotPointEmbed(User target, TextChannel channel) {
        return CompletableFuture.supplyAsync(() -> SheetsHandler.getPlotPoints(target)
                .map(integer -> new EmbedBuilder().addField(UtilFunctions.getUsernameInChannel(target, channel), String.valueOf(integer)))
                .orElseGet(this::getUnableToRetrieveEmbed));
    }

    private EmbedBuilder getUnableToRetrieveEmbed() {
        return new EmbedBuilder().setDescription("Unable to retrieve plot points!");
    }

    private CompletableFuture<EmbedBuilder> setPointsAndGetEmbed(User target, Integer count, TextChannel channel) {
        final Optional<Integer> plotPoints = SheetsHandler.getPlotPoints(target);
        if (plotPoints.isPresent()) {
            return SheetsHandler.setPlotPoints(target, count).thenApply(integer -> {
                if (integer.isPresent()) {
                    return new EmbedBuilder().addField(UtilFunctions.getUsernameInChannel(target, channel), plotPoints.get() + " → " + integer.get());
                }
                else {
                    return new EmbedBuilder().setDescription("Unable to set plot points!");
                }
            });
        }
        else {
            return CompletableFuture.completedFuture(getUnableToRetrieveEmbed());
        }
    }

    private CompletableFuture<EmbedBuilder> addPointsAndGetEmbed(User target, Integer count, TextChannel channel) {
        final Optional<Integer> plotPoints = SheetsHandler.getPlotPoints(target);
        if (plotPoints.isPresent()) {
            return PlotPointUtils.addPlotPointsToUser(target, count).thenApply(integer -> integer
                    .map(newPoints -> new EmbedBuilder().addField(UtilFunctions.getUsernameInChannel(target, channel), plotPoints.get() + " → " + integer.get()))
                    .orElseGet(() -> new EmbedBuilder().setDescription("Unable to set plot points for " + UtilFunctions.getUsernameInChannel(target, channel) + "!")));
        }
        else {
            return CompletableFuture.completedFuture(getUnableToRetrieveEmbed());
        }
    }

}
