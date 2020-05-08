package commands;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import dicerolling.NewDiceRoller;
import dicerolling.PoolProcessor;
import logic.PlotPointEnhancementHelper;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;

public class TestRollCommand implements CommandExecutor {
    @Command(aliases = {"~t", "~test"}, description = "A command that allows you to roll dice without automatically subtracting plot points or doom points.\n\tdie: A string representing a die. Some die examples are d4, pd12, 3d12, kd12, +3.", privateMessages = false, usage = "~t die|skill [die|skill ...]")
    public void onCommand(Message message, MessageAuthor author, TextChannel channel) {
        String messageContent = message.getContent();
        final PoolProcessor poolProcessor = new PoolProcessor(messageContent, author);

        NewDiceRoller newDiceRoller = new NewDiceRoller(poolProcessor.getDicePool());
        final EmbedBuilder resultEmbed = newDiceRoller.generateResults(author);
        new MessageBuilder()
                .setEmbed(resultEmbed)
                .send(channel)
                .thenAcceptAsync(new PlotPointEnhancementHelper()::addPlotPointEnhancementEmojis);
    }
}
