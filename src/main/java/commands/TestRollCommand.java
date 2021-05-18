package commands;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import dicerolling.DicePool;
import dicerolling.PoolProcessor;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.MessageBuilder;

public class TestRollCommand implements CommandExecutor {
    @Command(aliases = {"~t", "~test"}, description = "A command that allows you to roll dice without automatically subtracting plot points or doom points.\n\tdie: A string representing a die. Some die examples are d4, pd12, 3d12, kd12, +3.", privateMessages = false, usage = "~t die|skill [die|skill ...]")
    public void onCommand(Message message, MessageAuthor author, TextChannel channel) {
        String messageContent = message.getContent();
        final PoolProcessor poolProcessor = new PoolProcessor(author, messageContent);
        poolProcessor.getDicePool().setOpportunitiesEnabled(false);

        if (poolProcessor.getErrorEmbed() != null) {
            new MessageBuilder().setEmbed(poolProcessor.getErrorEmbed()).send(channel);
        }
        else {
            final DicePool dicePool = poolProcessor.getDicePool();
            RollCommand.rollPoolAndSend(channel, message, dicePool);
        }
    }
}
