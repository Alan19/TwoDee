package commands;

import com.vdurmont.emoji.EmojiParser;
import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import logic.CommandProcessor;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.MessageBuilder;
import statistics.StatisticsContext;


public class StatisticsCommand implements CommandExecutor {
    @Command(aliases = {"~s", "~stats", "~stat", "~statistics"}, description = "Generates an embed of roll probabilities based on dice input!", async = true, privateMessages = false)
    public void onCommand(String command, TextChannel channel, MessageAuthor author){
        String processedCommand = new CommandProcessor(author, channel).handleCommand(command.replaceAll("\\s+", " "));
        StatisticsContext context = new StatisticsContext(processedCommand);
        new MessageBuilder().setEmbed(context.getEmbedBuilder()).send(channel).thenAcceptAsync(message -> message.addReaction(EmojiParser.parseToUnicode(":x:")));
    }
}
