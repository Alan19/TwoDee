package commands;

import de.btobastian.sdcf4j.Command;
import util.RandomColor;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;

public class SnackCommand {
    @Command(aliases = {"~snack", "~snac"}, description = "Gives you a snack?", usage = "~snack")
    public void dispenseSnack(MessageAuthor author, TextChannel channel) {
        new MessageBuilder().setEmbed(new EmbedBuilder()
                .setColor(RandomColor.getRandomColor())
                .setAuthor(author)
                .setTitle("A snack for " + author.getDisplayName())
                .setDescription("Here is a cookie!")
                .setImage("https://upload.wikimedia.org/wikipedia/commons/5/5c/Choc-Chip-Cookie.png"))
                .send(channel);
    }
}
