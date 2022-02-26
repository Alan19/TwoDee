package logic;

import discord.TwoDee;
import doom.DoomHandler;
import io.vavr.control.Try;
import org.apache.commons.lang3.tuple.Pair;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.interaction.callback.InteractionCallbackDataFlag;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;
import pw.mihou.velen.interfaces.*;
import rolling.Result;
import rolling.RollOutput;
import rolling.RollParameters;
import rolling.Roller;
import sheets.PlotPointUtils;
import sheets.SheetsHandler;
import util.ComponentUtils;
import util.RandomColor;
import util.UtilFunctions;

import javax.annotation.Nullable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Rolls a pool of dice based on the input. After rolling, adds doom points to doom pool and makes appropriate changes the player's plot point count based on input options. If the DM is rolling, plot points they spend come from the doom point pool.
 */
public class RollLogic implements VelenSlashEvent, VelenEvent {

    public static void setupRollCommand(Velen velen) {
        RollLogic rollLogic = new RollLogic();
        final List<SlashCommandOption> rollCommandOptions = getRollCommandOptions();
        rollCommandOptions.add(SlashCommandOption.create(SlashCommandOptionType.BOOLEAN, "opportunity", "Allows for opportunities on the roll. Defaults to true.", false));
        VelenCommand.ofHybrid("roll", "Rolls some dice!", velen, rollLogic, rollLogic)
                .addOptions(rollCommandOptions.toArray(new SlashCommandOption[0]))
                .addShortcuts("r")
                .attach();
    }

    static List<SlashCommandOption> getRollCommandOptions() {
        List<SlashCommandOption> options = new ArrayList<>();
        options.add(SlashCommandOption.create(SlashCommandOptionType.STRING, "dicepool", "The dice pool to roll with.", true));
        options.add(SlashCommandOption.create(SlashCommandOptionType.LONG, "discount", "The number of plot points to discount (negative results in a plot point cost increase).", false));
        options.add(SlashCommandOption.create(SlashCommandOptionType.LONG, "dicekept", "The number of dice kept. Keeps two dice by default.", false));
        options.add(SlashCommandOption.create(SlashCommandOptionType.BOOLEAN, "enhanceable", "Allows the roll to be enhanced after the roll.", false));
        return options;
    }

    /**
     * Handles a roll made through a slash command
     *
     * @param event       The event that contains the user, channel, and responder objects
     * @param dicePool    The dicepool the user is rolling
     * @param discount    The plot point discount on the roll
     * @param diceKept    The number of dice kept
     * @param enhanceable If there is an enhancement override on the roll
     * @param opportunity If opportunities are enabled
     */
    public static void handleSlashCommandRoll(SlashCommandInteraction event, String dicePool, Integer discount, Integer diceKept, @Nullable Boolean enhanceable, Boolean opportunity) {
        final User user = event.getUser();
        final RollParameters rollParameters = new RollParameters(dicePool, discount, enhanceable, opportunity, diceKept);
        final CompletableFuture<InteractionOriginalResponseUpdater> updaterFuture = event.respondLater();
        Optional<Pair<Integer, Integer>> originalPointPair = SheetsHandler.getPlotPoints(user)
                .flatMap(integer -> DoomHandler.getUserDoomPool(user)
                        .map(s -> Pair.of(integer, DoomHandler.getDoom(s)))
                );
        rollDice(dicePool, discount, diceKept, opportunity, user, UtilFunctions.getUsernameFromSlashEvent(event, user))
                .onFailure(throwable -> event.getChannel().ifPresent(channel -> updaterFuture.thenAccept(updater -> updater
                        .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                        .setContent(throwable.getMessage())
                        .update())))
                .onSuccess(rollOutput -> updaterFuture.thenCompose(updater -> updater.addEmbeds(rollOutput.getEmbeds())
                        .addComponents(ComponentUtils.createRollComponentRows(true, Optional.ofNullable(enhanceable).orElse(rollOutput.isPlotDiceUsed()), rollOutput.getRollTotal()))
                        .update()
                        .thenAccept(message -> Roller.attachEmotesAndListeners(user, rollParameters, originalPointPair.orElse(Pair.of(0, 0)), rollOutput, message)))
                );
    }

    /**
     * A shortcut for rolling dice, updating the plot points and doom points, and generating the embed. A boolean is returned since the Result object is not carried to the next steps, and we need some way to check if a roll is enhanceable.
     *
     * @param dicePool    The dice pool to be rolled, skills are converted into dice
     * @param discount    The discount on plot points in the roll
     * @param diceKept    The amount of dice kept
     * @param opportunity If the roll can have opportunities
     * @param user        The user who made the roll
     * @return A CompletableFuture that contains a Triple that that has a list of the embeds to send, a boolean states if the roll uses any plot dice, and the total of the roll
     */
    public static Try<RollOutput> rollDice(String dicePool, Integer discount, Integer diceKept, Boolean opportunity, User user, String username) {
        return Roller.parse(dicePool, pool -> convertSkillsToDice(user, pool))
                .map(Roller::roll)
                .map(pair -> new Result(pair.getLeft(), pair.getRight(), diceKept))
                // TODO Convert to CompletableFuture earlier
                .map(result -> Roller.handlePoints(result, discount, opportunity, value -> DoomHandler.addDoomOnOpportunity(user, value), value -> PlotPointUtils.addPlotPointsOnRoll(user, value)))
                .map(changes -> Roller.output(changes, builder -> postProcessResult(dicePool, user, builder, username), DoomHandler.getDoomPoolOrDefault(user), discount, opportunity));
    }

    /**
     * Modifies the embed generated on a roll to include information about the user, a random color, and a random roll title
     *
     * @param dicePool The pool of dice that was rolled, after conversion
     * @param user     The user that rolled
     * @param builder  The EmbedBuilder that contains the results of the dice roll
     * @param username The display name for the user, generally their nickname
     * @return An embed that has information about the user and the dice pool added to it
     */
    private static EmbedBuilder postProcessResult(String dicePool, User user, EmbedBuilder builder, String username) {
        // TODO Use server profile and nicknames
        final Try<String> processedDicePool = Roller.preprocessPool(dicePool, s -> convertSkillsToDice(user, s));
        return builder.setAuthor(username, null, user.getAvatar())
                .setColor(RandomColor.getRandomColor())
                .setTitle(TwoDee.getRollTitleMessage())
                .setDescription(MessageFormat.format("Here are the results for **{0}**", processedDicePool.getOrElse(dicePool)));
    }

    /**
     * Converts skills into dice by replacing it with text from the spreadsheets. For example, if your pool is 2d8 stealth, and stealth is designated as 2d8 in the character sheet, the pool becomes 2d8 2d8.
     *
     * @param user The user whose character sheet will be checked
     * @param pool The pool that entered
     * @return A Try that attempts to convert a string with a skill into a string with only dice, or an exception if a skill cannot be found
     */
    private static Try<String> convertSkillsToDice(User user, String pool) {
        // We use join to get access to CompletionException, which would allow us to handle the exceptions thrown in the CompletableFuture
        return Try.of(() -> SheetsHandler.getSkills(user).get())
                .map(map -> Arrays.stream(pool.split(" ")).map(s -> map.getOrDefault(s, s)).collect(Collectors.joining(" ")));
    }

    /**
     * Removes the footer of the first embed in a message. Used for removing the 60 second warning from a roll embed.
     *
     * @param interactionMessage The message to remove the footer for
     * @return The list of embeds in that message with the first embed not having a footer
     */
    public static EmbedBuilder[] removeFirstEmbedFooter(Message interactionMessage) {
        EmbedBuilder[] afterEnhanceEmbed = interactionMessage.getEmbeds().stream().map(Embed::toBuilder).toArray(EmbedBuilder[]::new);
        if (afterEnhanceEmbed.length > 0) {
            afterEnhanceEmbed[0] = afterEnhanceEmbed[0].setFooter("");
        }
        return afterEnhanceEmbed;
    }

    /**
     * Handles a text roll by rolling the dice pool, generating the embed, attaching the listener.
     * The text command only allows for keeping 2 dice, can't specify discount, no enhancement override
     *
     * @param user        The user who made the roll
     * @param channel     The channel the message was sent from
     * @param pool        The pool to be rolled
     * @param opportunity If opportunities are enabled
     */
    public static void handleTextCommandRoll(User user, TextChannel channel, String pool, boolean opportunity) {
        Optional<Pair<Integer, Integer>> originalPointPair = SheetsHandler.getPlotPoints(user).flatMap(integer -> DoomHandler.getUserDoomPool(user).map(s -> Pair.of(integer, DoomHandler.getDoom(s))));
        final RollParameters rollParameters = new RollParameters(pool, 0, null, true, 2);
        rollDice(pool, 0, 2, opportunity, user, UtilFunctions.getUsernameInChannel(user, channel))
                .onFailure(throwable -> channel.sendMessage(throwable.getMessage()))
                .onSuccess(rollOutput -> new MessageBuilder()
                        .addEmbeds(rollOutput.getEmbeds())
                        .addComponents(ComponentUtils.createRollComponentRows(true, rollOutput.isPlotDiceUsed(), rollOutput.getRollTotal()))
                        .send(channel)
                        .thenAccept(message -> Roller.attachEmotesAndListeners(user, rollParameters, originalPointPair.orElse(Pair.of(0, 0)), rollOutput, message)));
    }

    @Override
    public void onEvent(MessageCreateEvent event, Message message, User user, String[] args) {
        String dicePool = String.join(" ", args);
        handleTextCommandRoll(user, event.getChannel(), dicePool, true);
    }

    @Override
    public void onEvent(SlashCommandInteraction event, User user, VelenArguments args, List<SlashCommandInteractionOption> options, InteractionImmediateResponseBuilder firstResponder) {
        final Boolean opportunity = event.getOptionBooleanValueByName("opportunity").orElse(true);
        final Long discount = event.getOptionLongValueByName("discount").orElse(0L);
        final String dicePool = event.getOptionStringValueByName("dicepool").orElse("");
        final Long diceKept = event.getOptionLongValueByName("dicekept").orElse(2L);
        final Boolean enhanceable = event.getOptionBooleanValueByName("enhanceable").orElse(null);

        handleSlashCommandRoll(event, dicePool, Math.toIntExact(discount), Math.toIntExact(diceKept), enhanceable, opportunity);
    }

}
