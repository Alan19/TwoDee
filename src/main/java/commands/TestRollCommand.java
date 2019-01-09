package commands;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import dicerolling.DiceRoller;
import logic.CommandProcessor;
import logic.PlotPointEnhancementHelper;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.MessageBuilder;

public class TestRollCommand implements CommandExecutor {
    @Command(aliases = {"~t", "~test"}, description = "A command that allows you to roll dice without automatically subtracting plot points or doom points.", privateMessages = false, usage = "~t die|skill [die|skill ...]\n\tdie: A string representing a die. Some die examples are d4, pd12, 3d12, kd12, +3.")
    public void onCommand(Message message, MessageAuthor author, TextChannel channel){
        String messageContent = message.getContent();
        String processedMessage = new CommandProcessor(author, channel).handleCommand(messageContent);
        DiceRoller roller = new DiceRoller(processedMessage);
        new MessageBuilder()
                .setEmbed(roller.generateResults(author))
                .send(channel)
                .thenAcceptAsync(new PlotPointEnhancementHelper()::addPlotPointEnhancementEmojis);
    }
}
