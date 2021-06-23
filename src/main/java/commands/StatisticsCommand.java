package commands;

import com.vdurmont.emoji.EmojiParser;
import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import util.RandomColor;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageDecoration;
import statistics.StatisticsContext;

import java.util.concurrent.CompletableFuture;


public class StatisticsCommand implements CommandExecutor {
    public static CompletableFuture<Void> addCancelReactToMessage(Message sentMessage) {
        return sentMessage.addReaction(EmojiParser.parseToUnicode(":x:"));
    }

    @Command(aliases = {"~s", "~stats", "~stat", "~statistics"}, description = "Generates an embed of roll probabilities based on dice input!\n\tdie: A string representing a die. Some die examples are d4, pd12, 3d12.\n The program will only support dice combinations where the product of all the die sizes are less than 12 to the 6th power.", async = true, privateMessages = false, usage = "~s die|skill [die|skill ...] [*here]")
    public void onCommand(Message message, TextChannel channel, MessageAuthor author, DiscordApi api) {
        StatisticsContext context = new StatisticsContext(message);
        if (message.getContent().contains("*here")) {
            channel.sendMessage("Here are the statistics for **" + message.getContent() + "**", context.getEmbedBuilder().setColor(RandomColor.getRandomColor())).thenAcceptAsync(StatisticsCommand::addCancelReactToMessage);
        }
        else {
            api.getUserById(author.getIdAsString()).thenAcceptAsync(user -> {
                user.sendMessage("Here are the statistics for **" + message.getContent() + "**", context.getEmbedBuilder().setColor(RandomColor.getRandomColor())).thenAcceptAsync(StatisticsCommand::addCancelReactToMessage);
                CompletableFuture<Message> statisticsPM = new MessageBuilder()
                        .setContent("Sent you a PM with your statistics for ")
                        .append(message.getContent(), MessageDecoration.BOLD)
                        .append(" " + user.getMentionTag())
                        .send(channel);
                statisticsPM.thenAcceptAsync(sentPM -> message.delete());
            });
        }
    }
}
