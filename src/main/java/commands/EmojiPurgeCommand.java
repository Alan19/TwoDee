package commands;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import discord.TwoDee;
import logic.PlotPointEnhancementHelper;
import logic.RandomColor;
import org.apache.commons.math3.util.Pair;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.Reaction;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EmojiPurgeCommand implements CommandExecutor {
    @Command(async = true, aliases = {"~emojipurge", "~killeverylastoneofthem", "~scorchedearth", "~removeemojis", "~p"}, description = "Removes all plot point enhancement emojis from this channel", usage = "~removeemojis [all]")
    public void purgeEmojis(String[] params, DiscordApi api, MessageAuthor author, Message message, TextChannel channel, Server server) {
        boolean removeFromAllChannels = params.length == 1 && params[0].equals("all") || message.getContent().equals("~killeverylastoneofthem") || message.getContent().equals("~scorchedearth");
        AtomicInteger totalMessages = new AtomicInteger();
        AtomicInteger totalEmojis = new AtomicInteger();
        if (removeFromAllChannels) {
            //Remove all of the emojis and then map the TextChannels to a stream
            server.getTextChannels().stream().map(this::removeAllPlotPointEmojisFromChannel).forEach(emojiInfoPair -> {
                totalMessages.addAndGet(emojiInfoPair.getKey());
                totalEmojis.addAndGet(emojiInfoPair.getValue());
                CompletableFuture<Message> sentEmbed = new MessageBuilder()
                        .setEmbed(new EmbedBuilder()
                                .setColor(RandomColor.getRandomColor())
                                .setAuthor(author)
                                .setTitle(TwoDee.getServerwideEmojiRemovalMessage())
                                .addField("Messages with Emojis", String.valueOf(totalMessages))
                                .addField("Emojis Removed", String.valueOf(totalEmojis))
                                .setDescription("I removed all roll enhancement emojis in all channels."))
                        .send(channel);
                sentEmbed.thenAcceptAsync(StatisticsCommand::addCancelReactToMessage);

            });
        } else {
            removeAllPlotPointEmojisFromChannel(channel);
        }
    }

    /**
     * Removes all enhancement emojis in a channel
     *
     * @param channel The channel to remove emojis from
     * @return A pair with the left side as the number of messages and the right side as the number of emojis
     */
    private Pair<Integer, Integer> removeAllPlotPointEmojisFromChannel(TextChannel channel) {
        ArrayList<Message> enhancementEmojiMessageList = getMessagesWithEnhancementEmojis(channel);
        int messagesToClear = enhancementEmojiMessageList.size();
        int emojis = getNumberOfEmojisToRemove(enhancementEmojiMessageList);
            Message progressMessage = new MessageBuilder().setContent("Removing " + emojis + " emojis from " + messagesToClear + " messages!").send(channel).join();
            AtomicInteger current = new AtomicInteger();
            DecimalFormat df = new DecimalFormat("0.##");
            enhancementEmojiMessageList.forEach(message -> {
                PlotPointEnhancementHelper.removeEnhancementEmojis(message);
                current.getAndIncrement();
                System.out.println(current + "/" + messagesToClear + " (" + ((double) current.get() / messagesToClear * 100) + "%)");
                progressMessage.edit("Removing " + emojis + " emojis from " + messagesToClear + " messages! " + current + "/" + messagesToClear + " (" + df.format((double) current.get() / messagesToClear * 100) + "%)");
            });
        //Add delete emoji when done
        progressMessage.edit(progressMessage.getContent() + "\nDone!");
        StatisticsCommand.addCancelReactToMessage(progressMessage);
        return new Pair<>(messagesToClear, emojis);
    }

    /**
     * Gets the number of enhancement emoji in the list of messages
     *
     * @param messageList The list of messages to search
     */
    private int getNumberOfEmojisToRemove(ArrayList<Message> messageList) {
        PlotPointEnhancementHelper helper = new PlotPointEnhancementHelper();
        AtomicInteger emojisInList = new AtomicInteger();
        messageList.stream()
                .map(Message::getReactions)
                .forEach(reactions -> reactions.stream()
                        .filter(Reaction::containsYou)
                        .filter(reaction -> helper.isEmojiEnhancementEmoji(reaction.getEmoji()))
                        .forEach(reaction -> emojisInList.addAndGet(reaction.getCount())));
        return emojisInList.get();
    }

    /**
     * Gets all of the emojis with enhancement emojis
     *
     * @param channel The channel to search for emojis
     * @return An arraylist of messages with enhancement emojis
     */
    private ArrayList<Message> getMessagesWithEnhancementEmojis(TextChannel channel) {
        PlotPointEnhancementHelper helper = new PlotPointEnhancementHelper();

        Stream<Message> allMessagesInChannel = channel.getMessagesAsStream();
        Stream<Message> filteredStream = allMessagesInChannel
                .filter(message1 -> message1.getReactions()
                        .stream()
                        .map(Reaction::getEmoji)
                        .anyMatch(helper::isEmojiEnhancementEmoji));
        return filteredStream.collect(Collectors.toCollection(ArrayList::new));
    }
}
