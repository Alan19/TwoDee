package logic;

import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.awt.image.BufferedImage;

public class SnackCommand {

    private MessageAuthor author;

    public SnackCommand(MessageAuthor author) {
        this.author = author;
    }

    public EmbedBuilder dispenseSnack(){
        return new EmbedBuilder()
                .setColor(RandomColor.getRandomColor())
                .setAuthor(author)
                .setTitle("A snack for " + author.getDisplayName())
                .setDescription("Here is a cookie!")
                .setImage("https://upload.wikimedia.org/wikipedia/commons/5/5c/Choc-Chip-Cookie.png");
    }
}
