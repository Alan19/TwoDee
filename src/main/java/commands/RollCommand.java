package commands;

import com.vdurmont.emoji.EmojiParser;
import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import dicerolling.DicePool;
import dicerolling.DiceRoller;
import dicerolling.PoolProcessor;
import dicerolling.SuccessCalculatorEmbed;
import doom.DoomWriter;
import logic.PlotPointEnhancementHelper;
import logic.RandomColor;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import sheets.PlotPointManager;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

public class RollCommand implements CommandExecutor {

    /**
     * Rolls a pool of dice based on the input. After rolling, adds doom points to doom pool and makes appropriate changes the player's plot point count based on input options. If the DM is rolling, plot points they spend come from the doom point pool.
     *
     * @param author  The author of the message
     * @param message The message containing the command
     * @param channel The channel the message was sent from
     */
    @Command(aliases = {"~r", "~roll"}, description = "A command that allows you to roll dice\n\tdie: A string representing a die. Some die examples are d4, pd12, 3d12, kd12, +3.\n\tskill: The value of a cell from a character's spreadsheet with no spaces and all lowercase.", privateMessages = false, usage = "~r [-fsu=x|-fsd=x|-maxf=x|-diff=|-k=x|-pdisc=x|-enh=true/false|-opp=true/false|-nd=pd/d/kd|-minf=x] die|skill [die|skill ...]", showInHelpPage = false)
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
            final CompletableFuture<Message> sentMessage = new MessageBuilder()
                    .setEmbed(diceRoller.generateResults(message.getAuthor()))
                    .send(channel);


            sentMessage.thenAcceptAsync(resultEmbed -> {
                try {
                    handleMessageSideEffects(message, dicePool, diceRoller, resultEmbed);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private void handleMessageSideEffects(Message message, DicePool dicePool, DiceRoller diceRoller, Message resultEmbed) throws IOException {
        Properties prop = new Properties();
        prop.load(new FileInputStream("resources/bot.properties"));
        final int plotPointsSpent = dicePool.getPlotPointsSpent() - dicePool.getPlotPointDiscount();
        MessageAuthor author = message.getAuthor();
        TextChannel channel = message.getChannel();

        //DMs use doom points as plot points and 1s do not increase the doom pool
        if (author.getIdAsString().equals(prop.getProperty("gameMaster"))) {
            if (plotPointsSpent != 0) {
                DoomWriter writer = new DoomWriter();
                EmbedBuilder doomEmbed = writer.addDoom(plotPointsSpent * -1);
                channel.sendMessage(doomEmbed);
            }
        }
        //Players have to spend plot points and gain doom points on opportunities
        else {
            if (dicePool.isEnableOpportunities() && diceRoller.getDoom() >= 1) {
                resultEmbed.addReaction(EmojiParser.parseToUnicode(":eight_pointed_black_star:"));
                new MessageBuilder().setEmbed(addOnePlotPointAndGenerateEmbed(author));
            }
            if (plotPointsSpent != 0) {
                new MessageBuilder().setEmbed(deductPlotPoints(plotPointsSpent, author)).send(channel);
            }
        }
        if (!dicePool.getDifficulty().equals("")) {
            new MessageBuilder().setEmbed(SuccessCalculatorEmbed.generateDifficultyEmbed(dicePool.getDifficulty(), diceRoller.getTotal(), author)).send(channel);
        }
        if (dicePool.isEnableEnhancementEmojis()) {
            PlotPointEnhancementHelper.addPlotPointEnhancementEmojis(resultEmbed);
        }
    }

    /**
     * Generate the embed that shows the player gaining 1 plot point. Used when a player rolls a 1.
     *
     * @param author The player that rolled a 1
     * @return The EmbedBuilder that shows the change in plot points for the player
     */
    public EmbedBuilder addOnePlotPointAndGenerateEmbed(MessageAuthor author) {
        String userID = author.getIdAsString();
        int oldPP = PlotPointManager.getPlotPoints(userID);
        int newPP = PlotPointManager.setPlotPoints(userID, oldPP + 1);
        return new EmbedBuilder()
                .setAuthor(author)
                .setTitle("Gambling with fate...")
                .setDescription(oldPP + " → " + newPP);

    }

    public EmbedBuilder deductPlotPoints(int plotPointsSpent, MessageAuthor author) {
        String authorID = author.getIdAsString();
        int previous = PlotPointManager.getPlotPoints(authorID);
        int current = PlotPointManager.setPlotPoints(authorID, PlotPointManager.getPlotPoints(authorID) - plotPointsSpent);
        return new EmbedBuilder()
                .setAuthor(author)
                .setTitle("Plot points!")
                .setColor(RandomColor.getRandomColor())
                .setDescription(previous + " → " + current);
    }

}
