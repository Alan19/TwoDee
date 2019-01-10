package logic;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;

public class CommandHandler {

    private DiscordApi api;
    private TextChannel channel;
    private MessageAuthor author;


    public CommandHandler(String content, MessageAuthor author, TextChannel channel, DiscordApi api) {
        this.author = author;
        this.channel = channel;
        this.api = api;
        commandSelector(content);
        System.out.println(content);
    }

    private void commandSelector(String message) {
        message = message.replaceAll("\\s+", " ");
        String prefix = message.split(" ")[0];
        switch (prefix) {
             case "~snack":
                SnackCommand snackCommand = new SnackCommand(author);
                new MessageBuilder()
                        .setEmbed(snackCommand.dispenseSnack())
                        .send(channel);
                break;

            default:
                return;
        }
        new EmbedBuilder()
                .setAuthor(author)
                .setDescription("Command not recognized");
    }










}
