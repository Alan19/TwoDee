package commands;

import com.vdurmont.emoji.EmojiParser;
import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import dicerolling.DicePool;
import dicerolling.DiceRoller;
import dicerolling.PoolProcessor;
import dicerolling.SuccessCalculatorEmbed;
import doom.DoomHandler;
import logic.PlotPointEnhancementHelper;
import org.apache.commons.lang3.tuple.Triple;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import roles.Storytellers;
import sheets.PlotPointHandler;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class RollCommand implements CommandExecutor {
    /**
     * Re-rolls the previous roll. Plot points spent by the player are not spent again, plot and doom points from opportunites are reverted before the re-roll.
     *
     * @param channel  The channel the reroll is in
     * @param message  The message that started the reroll
     * @param dicePool The dice pool to be re-rolled
     * @param doom     The number of doom points generated by the previous roll
     */
    private static void reroll(TextChannel channel, Message message, DicePool dicePool, int doom) {
        // Revert opportunities
        DoomHandler.addDoom(doom * -1);
        PlotPointHandler.addPlotPointsToUser(dicePool.getPlotPointsSpent() * -1, new ArrayList<>(), new ArrayList<>(), message.getUserAuthor().get())
                .thenAccept(integer -> {
                    DiceRoller diceRoller = new DiceRoller(dicePool);
                    final CompletableFuture<Message> sentMessageFuture = new MessageBuilder()
                            .setEmbed(diceRoller.generateResults(message.getAuthor()))
                            .send(channel);
                    sentMessageFuture.thenAcceptAsync(sentMessage -> {
                        handleMessageSideEffects(message, dicePool, diceRoller, sentMessage);
                        if (dicePool.enableEnhancements()) {
                            sentMessage.getApi().getThreadPool().getScheduler().schedule(() -> {
                                PlotPointEnhancementHelper.removeEnhancementEmojis(sentMessage);
                                final EmbedBuilder embedWithoutFooter = sentMessage.getEmbeds().get(0).toBuilder().setFooter("");
                                sentMessage.edit(embedWithoutFooter);
                            }, 60, TimeUnit.SECONDS);
                        }
                    });
                });
    }

    private static void rollDice(TextChannel channel, Message message, DicePool dicePool) {
        DiceRoller diceRoller = new DiceRoller(dicePool);
        final CompletableFuture<Message> sentMessageFuture = new MessageBuilder()
                .setEmbed(diceRoller.generateResults(message.getAuthor()))
                .send(channel);
        sentMessageFuture.thenAcceptAsync(sentMessage -> {
            handleMessageSideEffects(message, dicePool, diceRoller, sentMessage);
            attachRerollReaction(message, dicePool, diceRoller, sentMessage, channel);
            if (dicePool.enableEnhancements()) {
                sentMessage.getApi().getThreadPool().getScheduler().schedule(() -> {
                    PlotPointEnhancementHelper.removeEnhancementEmojis(sentMessage);
                    final EmbedBuilder embedWithoutFooter = sentMessage.getEmbeds().get(0).toBuilder().setFooter("");
                    sentMessage.edit(embedWithoutFooter);
                }, 60, TimeUnit.SECONDS);
            }
        });
    }

    /**
     * Handles the side effects after rolling a pool of dice such as modifying the doom pool and plot point pool
     *
     * @param userMessage The message that was sent by a Discord user
     * @param dicePool    The dice pool that was rolled
     * @param diceRoller  The DiceRoller object with information about the result of the dice rolled
     * @param sentMessage The message with the embed containing the roll result
     */
    public static void handleMessageSideEffects(Message userMessage, DicePool dicePool, DiceRoller diceRoller, Message sentMessage) {
        final int plotPointsSpent = dicePool.getPlotPointsSpent() - dicePool.getPlotPointDiscount();
        MessageAuthor author = userMessage.getAuthor();
        TextChannel channel = userMessage.getChannel();
        //DMs use doom points as plot points and 1s do not increase the doom pool
        if (author.asUser().isPresent() && Storytellers.isUserStoryteller(author.asUser().get())) {
            if (plotPointsSpent != 0) {
                EmbedBuilder doomEmbed = DoomHandler.addDoom(plotPointsSpent * -1);
                channel.sendMessage(doomEmbed);
            }
        }
        //Players have to spend plot points and gain doom points on opportunities
        else {
            handlePlayerRoll(sentMessage, author, channel, dicePool.enableOpportunities(), plotPointsSpent, diceRoller.getDoom());
        }
        if (!dicePool.getDifficulty().equals("")) {
            channel.sendMessage(SuccessCalculatorEmbed.generateDifficultyEmbed(dicePool.getDifficulty(), diceRoller.getTotal(), author));
        }
        if (dicePool.enableEnhancements()) {
            PlotPointEnhancementHelper.addPlotPointEnhancementEmojis(sentMessage);
        }
    }

    public static void attachRerollReaction(Message userMessage, DicePool dicePool, DiceRoller diceRoller, Message sentMessage, TextChannel channel) {
        sentMessage.addReaction(EmojiParser.parseToUnicode(":repeat:")).thenAccept(unused -> sentMessage.addReactionAddListener(event -> {
            if (event.getReaction().map(reaction -> reaction.getEmoji().equalsEmoji(EmojiParser.parseToUnicode(":repeat:"))).orElse(false)) {
                event.removeReaction().thenAccept(message -> reroll(channel, userMessage, dicePool, diceRoller.getDoom()));
            }
        }).removeAfter(60, TimeUnit.SECONDS));
    }

    /**
     * Rolls a pool of dice based on the input. After rolling, adds doom points to doom pool and makes appropriate changes the player's plot point count based on input options. If the DM is rolling, plot points they spend come from the doom point pool.
     *
     * @param author  The author of the message
     * @param message The message containing the command
     * @param channel The channel the message was sent from
     */
    @Command(aliases = {"~r", "~roll"}, description = "A command that allows you to roll dice\n\tdie: A string representing a die. Some die examples are d4, pd12, 3d12, kd12, +3.\n\tskill: The value of a cell from a character's spreadsheet with no spaces and all lowercase.", privateMessages = false, usage = "~r [-fsu=x|-fsd=x|-maxf=x|-diff=|-k=x|-pdisc=x|-enh=true/false|-opp=true/false|-nd=pd/d/kd|-minf=x] die|skill [die|skill ...]")
    public void onRollCommand(MessageAuthor author, Message message, TextChannel channel) {
        String messageContent = message.getContent();

        //Variables containing roll information
        final PoolProcessor poolProcessor = new PoolProcessor(messageContent, author);
        if (poolProcessor.getErrorEmbed() != null) {
            new MessageBuilder().setEmbed(poolProcessor.getErrorEmbed()).send(channel);
        }
        else {
            final DicePool dicePool = poolProcessor.getDicePool();
            rollDice(channel, message, dicePool);
        }
    }

    /**
     * Handles the modification of a player's plot points and the doom pool after the roll
     *
     * @param sentMessage         The message for the result of the roll
     * @param author              The player that made the roll
     * @param channel             The channel the roll was made in
     * @param enableOpportunities If opportunities are enabled
     * @param plotPointsSpent     The number of plot points spent in the roll
     * @param doomGenerated       How many 1s were rolled in that roll
     */
    private static void handlePlayerRoll(Message sentMessage, MessageAuthor author, TextChannel channel, boolean enableOpportunities, int plotPointsSpent, int doomGenerated) {
        if (plotPointsSpent != 0 && author.asUser().isPresent()) {
            ArrayList<Triple<User, Integer, Integer>> plotPointChanges = new ArrayList<>();
            PlotPointHandler.addPlotPointsToUser(plotPointsSpent * -1, plotPointChanges, new ArrayList<>(), author.asUser().get())
                    .thenAccept(integer -> channel.sendMessage(PlotPointHandler.generateEmbed(plotPointChanges, channel, author)
                            .setTitle(MessageFormat.format("Using {0} plot points!", plotPointsSpent)))).join();
        }
        // Send embed for plot points and doom if there's an opportunity
        if (enableOpportunities && doomGenerated >= 1) {
            sentMessage.addReaction(EmojiParser.parseToUnicode(":eight_pointed_black_star:"));
            EmbedBuilder doomEmbed = DoomHandler.addDoom(doomGenerated);
            if (author.asUser().isPresent()) {
                ArrayList<Triple<User, Integer, Integer>> plotPointChanges = new ArrayList<>();
                PlotPointHandler.addPlotPointsToUser(1, plotPointChanges, new ArrayList<>(), author.asUser().get())
                        .thenAccept(integer -> channel.sendMessage(PlotPointHandler.generateEmbed(plotPointChanges, channel, author).setTitle("An opportunity!")))
                        .join();
                doomEmbed.setFooter(MessageFormat.format("Generated by {0}!", PlotPointHandler.getUsernameInChannel(author.asUser().get(), channel)));
            }
            channel.sendMessage(doomEmbed);
        }
    }

}
