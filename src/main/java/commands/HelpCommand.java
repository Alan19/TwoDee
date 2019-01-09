package commands;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.embed.EmbedBuilder;

public class HelpCommand {
    private String message;
    private MessageAuthor author;

    public HelpCommand(String prefix, MessageAuthor author) {
        this.message = prefix;
        this.author = author;
    }

    public HelpCommand(MessageAuthor author) {
        this.author = author;
        this.message = "";
    }

    public EmbedBuilder getHelp() {
        EmbedBuilder helpEmbed = new EmbedBuilder();
        helpEmbed.setAuthor(author);
        switch (message){
            case "p":
            case "pp":
            case "plot":
            case "plotpoints":
                helpEmbed.setTitle("Plot points")
                        .setDescription("~p <name> [add|sub|addall|set] [number]");
                break;

            case "d":
            case "doom":
                helpEmbed.setTitle("Doom points")
                        .setDescription("~d <[add|sub|addall|set]> [number]");
                break;

            case "s":
            case "stat":
            case "stats":
            case "statistics":
                helpEmbed.setTitle("Dice statistics")
                        .setDescription("~s <d*number*> <pd*number>\n6 dice max");
                break;

            case "r":
            case "roll":
                helpEmbed.setTitle("Roll")
                        .setDescription("~r <d*number*> <pd*number>");
                break;

            case "~t":
            case "~test":
                helpEmbed.setTitle("Test roll")
                        .setDescription("~t <d*number*> <pd*number>");
                break;

            default:
                helpEmbed.setTitle("All commands")
                        .addField("Plot points", "~p <name> [add|sub|addall|set] [number]")
                        .addField("Doom points", "~d <[add|sub|addall|set]> [number]")
                        .addField("Dice statisitcs", "~s <d*number*> <pd*number>\n6 dice max")
                        .addField("Roll dice", "~r <d*number*> <pd*number>")
                        .addField("Test roll", "~t <d*number*> <pd*number>");
        }
        return helpEmbed;
    }
}
