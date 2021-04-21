package commands;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import dicerolling.DicePool;
import dicerolling.DiceRoller;
import dicerolling.PoolProcessor;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.MessageBuilder;

import java.util.concurrent.CompletableFuture;

public class TestRollCommand implements CommandExecutor {
    @Command(aliases = {"~t", "~test"}, description = "A command that allows you to roll dice without automatically subtracting plot points or doom points.\n\tdie: A string representing a die. Some die examples are d4, pd12, 3d12, kd12, +3.", privateMessages = false, usage = "~t die|skill [die|skill ...]")
    public void onCommand(Message message, MessageAuthor author, TextChannel channel) {
        String messageContent = message.getContent();

        //Variables containing roll information, disable opportunities and plot point costs
        final PoolProcessor poolProcessor = new PoolProcessor(messageContent, author);
        poolProcessor.getDicePool().setOpportunities(false);
        poolProcessor.getDicePool().setPlotPointDiscount(Integer.MAX_VALUE);
        if (poolProcessor.getErrorEmbed() != null) {
            new MessageBuilder().setEmbed(poolProcessor.getErrorEmbed()).send(channel);
        }
        else {
            final DicePool dicePool = poolProcessor.getDicePool();
            DiceRoller diceRoller = new DiceRoller(dicePool);
            final CompletableFuture<Message> sentMessageFuture = new MessageBuilder()
                    .setEmbed(diceRoller.generateResults(message.getAuthor()))
                    .send(channel);
            sentMessageFuture.thenAcceptAsync(sentMessage -> RollCommand.handleMessageSideEffects(message, dicePool, diceRoller, sentMessage));
        }
    }
}
