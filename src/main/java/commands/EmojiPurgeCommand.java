package commands;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import discord.TwoDee;
import listeners.PlotPointEnhancementListener;
import logic.PlotPointEnhancementHelper;
import logic.RandomColor;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class EmojiPurgeCommand implements CommandExecutor {
    private int emojis = 0;
    private int messagesToClear = 0;

    @Command(async = true, aliases = {"~emojipurge", "~killeverylastoneofthem"}, description = "Removes all plot point enhancement emojis from this channel", usage = "~removeemojis [all]")
    public void purgeEmojis(String[] params, DiscordApi api, MessageAuthor author, Message message, TextChannel channel, Server server) {
        boolean removeFromAllChannels = params.length == 1 && params[0].equals("all") || message.getContent().equals("~killeverylastoneofthem");
        if (removeFromAllChannels) {
            server.getTextChannels().forEach(this::removeAllPlotPointEmojisFromChannel);
        } else {
            removeAllPlotPointEmojisFromChannel(channel);
        }
        new MessageBuilder()
                .setEmbed(new EmbedBuilder()
                        .setColor(RandomColor.getRandomColor())
                        .setAuthor(author)
                        .setTitle(removeFromAllChannels ? TwoDee.getServerwideEmojiRemovalMessage() : TwoDee.getEmojiRemovalMessage())
                        .addField("Messages with Emojis", String.valueOf(messagesToClear))
                        .addField("Emojis Removed", String.valueOf(emojis))
                        .setDescription("I removed all roll enhancement emojis in " + (removeFromAllChannels ? "all channels." : "this channel.")))
                .send(channel);
    }

    /**
     * Removes all enhancement emojis in a channel
     *
     * @param channel The channel to remove emojis from
     */
    private void removeAllPlotPointEmojisFromChannel(TextChannel channel) {
        ArrayList<Message> enhancementEmojiMessageList = getMessagesWithEnhancementEmojis(channel);
        messagesToClear = enhancementEmojiMessageList.size();
        getNumberOfEmojisToRemove(enhancementEmojiMessageList);
        Message progressMessage = new MessageBuilder().setContent("Removing " + emojis + " emojis from " + messagesToClear + " messages!").send(channel).join();
        AtomicInteger current = new AtomicInteger();
        DecimalFormat df = new DecimalFormat("0.##");
        enhancementEmojiMessageList.forEach(message -> {
            PlotPointEnhancementListener.removeEnhancementEmojis(message);
            current.getAndIncrement();
            System.out.println(current + "/" + messagesToClear + " (" + ((double) current.get() / messagesToClear * 100) + "%)");
            progressMessage.edit("Removing " + emojis + " emojis from " + messagesToClear + " messages! " + current + "/" + messagesToClear + " (" + df.format((double) current.get() / messagesToClear * 100) + "%)");
        });
    }

    /**
     * Gets the number of enhancement emoji in the list of messages
     *
     * @param messageList The list of messages to search
     */
    private void getNumberOfEmojisToRemove(ArrayList<Message> messageList) {
        PlotPointEnhancementHelper helper = new PlotPointEnhancementHelper();
        messageList.stream()
                .map(Message::getReactions)
                .forEach(reactions -> reactions.stream()
                        .filter(Reaction::containsYou)
                        .filter(reaction -> helper.isEmojiEnhancementEmoji(reaction.getEmoji()))
                        .forEach(reaction -> emojis += reaction.getCount()));
    }

    /**
     * Gets all of the emojis with enhancement emojis
     *
     * @param channel The channel to search for emojis
     * @return An arraylist of messages with enhancement emojis
     */
    private ArrayList<Message> getMessagesWithEnhancementEmojis(TextChannel channel) {
        PlotPointEnhancementHelper helper = new PlotPointEnhancementHelper();

        return channel.getMessages(Integer.MAX_VALUE).join()
                .stream()
                .filter(message1 -> message1.getReactions()
                        .stream()
                        .map(Reaction::getEmoji)
                        .anyMatch(helper::isEmojiEnhancementEmoji))
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
