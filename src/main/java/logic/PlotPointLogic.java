package logic;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionBuilder;
import org.javacord.api.interaction.SlashCommandOptionType;
import pw.mihou.velen.interfaces.Velen;
import pw.mihou.velen.interfaces.VelenCommand;
import pw.mihou.velen.interfaces.VelenHybridHandler;
import pw.mihou.velen.interfaces.hybrid.event.VelenGeneralEvent;
import pw.mihou.velen.interfaces.hybrid.objects.VelenHybridArguments;
import pw.mihou.velen.interfaces.hybrid.objects.VelenOption;
import pw.mihou.velen.interfaces.hybrid.objects.subcommands.VelenSubcommand;
import pw.mihou.velen.interfaces.hybrid.responder.VelenGeneralResponder;
import sheets.PlotPointUtils;
import sheets.SheetsHandler;
import util.DiscordHelper;
import util.RandomColor;
import util.UtilFunctions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * This class adds plot points, subtracts plot points, and sets plot points for players. This class will also keep
 * track of doom points
 */
public class PlotPointLogic implements VelenHybridHandler {

    public static void registerPlotPointCommand(Velen velen) {
        final List<SlashCommandOption> options = new ArrayList<>();
        options.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "add", "Adds to the specified plot point pool(s)", getUserOption(), getPlotPointCountOption()));
        options.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "sub", "Subtracts from the specified plot point pool(s)", getUserOption(), getPlotPointCountOption()));
        options.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "set", "Sets the specified plot point pools to the specified amount", getPlotPointCountOption().setRequired(true), getUserOption()));
        options.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "query", "Queries the value of all plot point pools", getUserOption().setDescription("which player to query the plot point pool of")));

        PlotPointLogic plotPointLogic = new PlotPointLogic();
        VelenCommand.ofHybrid("plot", "Adjust plot points", velen, plotPointLogic)
                .addShortcuts("p", "pp", "plotpoints")
                .addFormats("plot :[mode:of(subcommand)]",
                        "plot add :[name:of(user)]",
                        "plot add :[count:of(numeric)]",
                        "plot add :[name:of(user)] :[count:of(numeric)]",
                        "plot sub :[name:of(user)]",
                        "plot sub :[count:of(numeric)]",
                        "plot sub :[name:of(user)] :[count:of(numeric)]",
                        "plot set :[count:of(numeric)]",
                        "plot set :[name:of(user)] :[count:of(numeric)]",
                        "plot :[name:of(user)]")
                .addOptions(options.toArray(new SlashCommandOption[]{}))
                .attach();
    }

    private static SlashCommandOptionBuilder getUserOption() {
        return new SlashCommandOptionBuilder()
                .setType(SlashCommandOptionType.USER)
                .setName("name")
                .setDescription("the user to target with the command")
                .setRequired(false);
    }

    private static SlashCommandOptionBuilder getPlotPointCountOption() {
        return new SlashCommandOptionBuilder()
                .setName("count")
                .setDescription("the amount to modify the plot point pool by")
                .setType(SlashCommandOptionType.LONG)
                .setRequired(false);
    }

    private CompletableFuture<EmbedBuilder> executeCommand(User sender, String mode, User target, Integer count, TextChannel channel) {
        CompletableFuture<EmbedBuilder> embed = switch (mode) {
            case "add" -> addPointsAndGetEmbed(target, count, channel);
            case "sub" -> addPointsAndGetEmbed(target, count * -1, channel);
            case "set" -> setPointsAndGetEmbed(target, count, channel);
            default -> getPlotPointEmbed(target, channel);
        };
        return embed.thenApply(builder -> builder.setTitle("Plot Points!")
                .setColor(RandomColor.getRandomColor())
                .setFooter("Requested by " + UtilFunctions.getUsernameInChannel(sender, channel), sender.getAvatar()));
    }

    private static EmbedBuilder getUnableToRetrieveEmbed() {
        return new EmbedBuilder().setDescription("Unable to retrieve plot points!");
    }

    public static CompletableFuture<EmbedBuilder> addPointsAndGetEmbed(User target, Integer count, TextChannel channel) {
        final Optional<Integer> plotPoints = SheetsHandler.getPlotPoints(target);
        if (plotPoints.isPresent()) {
            return PlotPointUtils.addPlotPointsToUser(target, count).thenApply(integer -> integer
                    .map(newPoints -> new EmbedBuilder().addField(UtilFunctions.getUsernameInChannel(target, channel), plotPoints.get() + " → " + integer.get()))
                    .orElseGet(() -> new EmbedBuilder().setDescription("Unable to set plot points for " + UtilFunctions.getUsernameInChannel(target, channel) + "!")));
        }
        else {
            return CompletableFuture.completedFuture(PlotPointLogic.getUnableToRetrieveEmbed());
        }
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

    private CompletableFuture<EmbedBuilder> getPlotPointEmbed(User target, TextChannel channel) {
        return CompletableFuture.supplyAsync(() -> SheetsHandler.getPlotPoints(target)
                .map(integer -> new EmbedBuilder().addField(UtilFunctions.getUsernameInChannel(target, channel), String.valueOf(integer)))
                .orElseGet(PlotPointLogic::getUnableToRetrieveEmbed));
    }

    @Override
    public void onEvent(VelenGeneralEvent event, VelenGeneralResponder responder, User user, VelenHybridArguments args) {
        final Optional<VelenSubcommand> subcommand = DiscordHelper.getSubcommandInHybridCommand(event.isMessageEvent(), args.getOptions());
        String subcommandName = subcommand.map(VelenSubcommand::getName).orElse("query");
        // Hacky workaround to still restrict input while allowing subcommands with no parameters
        if (Arrays.asList("add", "sub", "set", "query").contains(subcommandName)) {
            final int count = subcommand.flatMap(velenSubcommand -> velenSubcommand.withName("count"))
                    .flatMap(VelenOption::asInteger)
                    .orElse(1);
            final User target = subcommand.flatMap(velenSubcommand -> velenSubcommand.withName("name"))
                    .flatMap(VelenOption::asUser)
                    .orElse(user);
            executeCommand(user, subcommandName, target, count, event.getChannel()).thenAccept(output -> responder.addEmbed(output.setThumbnail(DiscordHelper.getLocalAvatar(event, target))).respond());
        }
    }
}
