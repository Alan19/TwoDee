package commands;

import com.vdurmont.emoji.EmojiParser;
import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import logic.CommandProcessor;
import logic.RandomColor;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.MessageBuilder;
import statistics.StatisticsContext;


public class StatisticsCommand implements CommandExecutor {
    @Command(aliases = {"~s", "~stats", "~stat", "~statistics"}, description = "Generates an embed of roll probabilities based on dice input!", async = true, privateMessages = false, usage = "~s die|skill [die|skill ...]\n\tdie: A string representing a die. Some die examples are d4, pd12, 3d12.\n The program will only support dice combinations where the product of all the die sizes are less than 12 to the 6th power.")
    public void onCommand(Message message, TextChannel channel, MessageAuthor author) {
        String processedCommand = new CommandProcessor(author, channel).handleCommand(message.getContent());
        StatisticsContext context = new StatisticsContext(processedCommand);
        new MessageBuilder()
                .setEmbed(context.getEmbedBuilder().setColor(RandomColor.getRandomColor()))
                .send(channel)
                .thenAcceptAsync(sentMessage -> sentMessage.addReaction(EmojiParser.parseToUnicode(":x:")));
    }
}
