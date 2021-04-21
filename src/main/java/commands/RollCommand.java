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
import logic.RandomColor;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import sheets.SheetsHandler;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

public class RollCommand implements CommandExecutor {

    /**
     * Handles the side effects after rolling a pool of dice such as modifying the doom pool and plot point pool
     *
     * @param userMessage The message that was sent by a Discord user
     * @param dicePool    The dice pool that was rolled
     * @param diceRoller  The DiceRoller object with information about the result of the dice rolled
     * @param sentMessage The message with the embed containing the roll result
     */
    public static void handleMessageSideEffects(Message userMessage, DicePool dicePool, DiceRoller diceRoller, Message sentMessage) {
        Properties prop = new Properties();
        try (FileInputStream stream = new FileInputStream("resources/bot.properties")) {
            prop.load(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        final int plotPointsSpent = dicePool.getPlotPointsSpent() - dicePool.getPlotPointDiscount();
        MessageAuthor author = userMessage.getAuthor();
        TextChannel channel = userMessage.getChannel();
        //DMs use doom points as plot points and 1s do not increase the doom pool
        if (author.getIdAsString().equals(prop.getProperty("gameMaster", ""))) {
            if (plotPointsSpent != 0) {
                EmbedBuilder doomEmbed = DoomHandler.addDoom(plotPointsSpent * -1);
                channel.sendMessage(doomEmbed);
            }
        }
        //Players have to spend plot points and gain doom points on opportunities
        else {
            if (dicePool.enableOpportunities() && diceRoller.getDoom() >= 1) {
                sentMessage.addReaction(EmojiParser.parseToUnicode(":eight_pointed_black_star:"));
                if (author.asUser().isPresent()) {
                    try {
                        channel.sendMessage(addOnePlotPointAndGenerateEmbed(author.asUser().get()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                EmbedBuilder doomEmbed = DoomHandler.addDoom(diceRoller.getDoom());
                channel.sendMessage(doomEmbed);
            }
            if (plotPointsSpent != 0 && author.asUser().isPresent()) {
                try {
                    channel.sendMessage(deductPlotPoints(plotPointsSpent, author.asUser().get()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (!dicePool.getDifficulty().equals("")) {
            channel.sendMessage(SuccessCalculatorEmbed.generateDifficultyEmbed(dicePool.getDifficulty(), diceRoller.getTotal(), author));
        }
        if (dicePool.enableEnhancements()) {
            PlotPointEnhancementHelper.addPlotPointEnhancementEmojis(sentMessage);
        }
    }

    /**
     * Rolls a pool of dice based on the input. After rolling, adds doom points to doom pool and makes appropriate changes the player's plot point count based on input options. If the DM is rolling, plot points they spend come from the doom point pool.
     *
     * @param author  The author of the message
     * @param message The message containing the command
     * @param channel The channel the message was sent from
     */
    @Command(aliases = {"~r", "~roll"}, description = "A command that allows you to roll dice\n\tdie: A string representing a die. Some die examples are d4, pd12, 3d12, kd12, +3.\n\tskill: The value of a cell from a character's spreadsheet with no spaces and all lowercase.", privateMessages = false, usage = "~r [-fsu=x|-fsd=x|-maxf=x|-diff=|-k=x|-pdisc=x|-enh=true/false|-opp=true/false|-nd=pd/d/kd|-minf=x] die|skill [die|skill ...]")
    public void onCommand(MessageAuthor author, Message message, TextChannel channel) {
        String messageContent = message.getContent();

        //Variables containing roll information
        final PoolProcessor poolProcessor = new PoolProcessor(messageContent, author);
        if (poolProcessor.getErrorEmbed() != null) {
            new MessageBuilder().setEmbed(poolProcessor.getErrorEmbed()).send(channel);
        }
        else {
            final DicePool dicePool = poolProcessor.getDicePool();

            DiceRoller diceRoller = new DiceRoller(dicePool);
            final CompletableFuture<Message> sentMessageFuture = new MessageBuilder()
                    .setEmbed(diceRoller.generateResults(message.getAuthor()))
                    .send(channel);


            sentMessageFuture.thenAcceptAsync(sentMessage -> handleMessageSideEffects(message, dicePool, diceRoller, sentMessage));
        }
    }

    /**
     * Generate the embed that shows the player gaining 1 plot point. Used when a player rolls a 1.
     *
     * @param author The player that rolled a 1
     * @return The EmbedBuilder that shows the change in plot points for the player
     */
    public static EmbedBuilder addOnePlotPointAndGenerateEmbed(User author) throws IOException {
        if (SheetsHandler.getPlotPoints(author).isPresent()) {
            int oldPP = SheetsHandler.getPlotPoints(author).get();
            final int newPP = oldPP + 1;
            SheetsHandler.setPlotPoints(author, newPP);
            return new EmbedBuilder()
                    .setAuthor(author)
                    .setTitle("An opportunity!")
                    .setDescription(oldPP + " → " + newPP);
        }
        else {
            return new EmbedBuilder()
                    .setAuthor(author)
                    .setDescription("I was unable to access the plot points of " + author.getName());
        }
    }

    /**
     * Deduct plot points from a player and generate an embed that shows the change in plot points
     *
     * @param plotPointsSpent The number of plot points being spent
     * @param author          The player who made the roll
     * @return The EmbedBuilder showing the change in plot points
     */
    public static EmbedBuilder deductPlotPoints(int plotPointsSpent, User author) throws IOException {
        final Optional<Integer> plotPoints = SheetsHandler.getPlotPoints(author);
        if (plotPoints.isPresent()) {
            final int newPlotPints = plotPoints.get() - plotPointsSpent;
            SheetsHandler.setPlotPoints(author, newPlotPints);
            return new EmbedBuilder()
                    .setAuthor(author)
                    .setTitle("Plot points!")
                    .setColor(RandomColor.getRandomColor())
                    .setDescription(plotPoints.get() + " → " + newPlotPints);
        }
        else {
            return new EmbedBuilder()
                    .setAuthor(author)
                    .setDescription("I was unable to access the plot points of " + author.getName());
        }
    }

}
